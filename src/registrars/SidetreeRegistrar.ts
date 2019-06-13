/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

require('es6-promise').polyfill();
import base64url from 'base64url';
import 'isomorphic-fetch';
import Identifier from '../Identifier';
import IdentifierDocument from '../IdentifierDocument';
import UserAgentError from '../UserAgentError';
import UserAgentOptions from '../UserAgentOptions';
import IRegistrar from './IRegistrar';
import Multihash from './Multihash';
import { ProtectionFormat } from '../crypto/keyStore/ProtectionFormat';
import CryptoFactory from '../crypto/plugin/CryptoFactory';
import JwsToken from '../crypto/protocols/jws/JwsToken';
import { TSMap } from 'typescript-map';
import { IJwsSigningOptions } from '../crypto/protocols/jose/IJoseOptions';
const cloneDeep = require('lodash/fp/cloneDeep');
declare var fetch: any;

/**
 * Registrar implementation for the Sidetree (ION) network
 */
export default class SidetreeRegistrar implements IRegistrar {
  private timeoutInMilliseconds: number;
  private serializedOptions: string;
  private cryptoFactory: CryptoFactory;

  /**
   * Constructs a new instance of the Sidetree registrar
   * @param url to the registration endpoint at the registrar
   * @param options to configure the registrar.
   */
  constructor (public url: string, options: UserAgentOptions) {
    // Set options. Stringify to avoid circular exception during serialization of this object.
    if (!(options && options.keyStore)) {
      throw new UserAgentError('options and options.keyStore need to be defined');
    }

    this.serializedOptions = JSON.stringify(options);
    this.cryptoFactory = options.cryptoFactory;

    // Format the url
    this.url = `${url.replace(/\/?$/, '/')}`;
    this.timeoutInMilliseconds =
      1000 *
      (!options || !options.timeoutInSeconds
        ? 30
        : options.timeoutInSeconds);
  }

  /**
   * Prepare the document for registration
   * @param document Document to format
   */
  public prepareDocForRegistration (document: IdentifierDocument): IdentifierDocument {
    delete document.id;
    return document;
  }

  /**
   * Registers the identifier document on the ledger
   * returning the identifier generated by the registrar.
   * @param identifierDocument to register.
   * @param keyReference Reference to the identifier for the signing key.
   */
  public async register (
    identifierDocument: IdentifierDocument,
    keyReference: string
  ): Promise<Identifier> {

    return new Promise(async (resolve, reject) => {
      const timer = setTimeout(
        () =>
          reject(new UserAgentError('Fetch timed out.')),
        this.timeoutInMilliseconds
      );

      // prepare document for registration
      identifierDocument = this.prepareDocForRegistration(identifierDocument);
      let bodyString = JSON.stringify(identifierDocument);
      console.debug(bodyString);

      // registration with signed message for bodyString
      const signingOptions: IJwsSigningOptions = {
        cryptoFactory: this.cryptoFactory,
        header: new TSMap<string, string>([
            ['alg', ''],
            ['kid', ''],
            ['operation', 'create'],
            ['proofOfWork', '{}']
        ])
      };
      const jws = new JwsToken(signingOptions);
      const signature = await jws.sign(keyReference, Buffer.from(bodyString), ProtectionFormat.JwsFlatJson);
      const encodedBodyString = signature.serialize();

      const fetchOptions = {
        method: 'POST',
        body: encodedBodyString,
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': encodedBodyString.length.toString()
        }
      };

      // Now call the actual fetch with the updated options
      const response = await fetch(this.url, fetchOptions);

      // Got a response so clear the timer
      clearTimeout(timer);

      if (!response.ok) {
        let msg: string;
        if (response.body) {
          msg = response.body.read().toString();;
        } else {
          msg = `Status ${response.status}`;
        }
        const error = new UserAgentError(
          `Failed to register the identifier document: ${msg}`
        );
        reject(error);
        return;
      }

      const responseJson = IdentifierDocument.fromJSON(await response.json());
      const identifier = new Identifier(responseJson, <UserAgentOptions> JSON.parse(this.serializedOptions));
      resolve(identifier);
    });
  }

  /**
   * Uses the specified input to create a basic Sidetree
   * compliant identifier document and then hashes the document
   * in accordance with the Sidetree protocol specification
   * to generate and return the identifier.
   *
   * @param identifierDocument for which to generate the identifier.
   */
  public async generateIdentifier (identifierDocument: IdentifierDocument
  ): Promise<Identifier> {

    if (!Array.isArray(identifierDocument.publicKeys) || identifierDocument.publicKeys.length === 0) {
      throw new UserAgentError('At least one public key must be specified in the identifier document.');
    }

    // The genesis document is used for generating the hash,
    // but we need to ensure that the id property of the document
    // if specified is removed beforehand.
    const genesisDocument = cloneDeep(identifierDocument);
    genesisDocument.id = undefined;

    // Hash the document JSON
    const documentBuffer = Buffer.from(JSON.stringify(genesisDocument));
    const hashedDocument = Multihash.hash(documentBuffer, 18);
    const encodedDocument = base64url.encode(hashedDocument);

    // Now update the identifier property in
    // the genesis document
    genesisDocument.id = `did:ion:test:${encodedDocument}`;
    return new Identifier(genesisDocument);
  }
}

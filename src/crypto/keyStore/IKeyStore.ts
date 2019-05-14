/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

import PrivateKey from '../keys/PrivateKey';
import PublicKey from '../keys/PublicKey';
import CryptoFactory from '../plugin/CryptoFactory';

/**
 * Define different types for the algorithm parameter
 */
export type CryptoAlgorithm = RsaPssParams | EcdsaParams | AesCmacParams;

/**
 * Interface defining IKeyStore options.
 */
export interface IKeyStoreOptions {
  // The crypto algorithm suites used for signing
  cryptoFactory: CryptoFactory,

  // The key store
  keyStore: IKeyStore,

  // The used algorithm
  algorithm?: CryptoAlgorithm,

  // The default protected header
  protected?: { [name: string]: string },

  // The default header
  header?: { [name: string]: string },

  // Make the type indexable
  [key: string]: any;
}

/**
 * Interface defining signature options.
 */
export interface ISigningOptions extends IKeyStoreOptions {
}

/**
 * Interface defining encryption options.
 */
export interface IEncryptionOptions extends IKeyStoreOptions {
  // The key encryption algorithm
  kekAlgorithm: Algorithm
}

/**
 * Interface defining methods and properties to
 * be implemented by specific key stores.
 */
export default interface IKeyStore {
  /**
   * Returns the key associated with the specified
   * key reference.
   * @param keyIdentifier for which to return the key.
   * @param publicKeyOnly True if only the public key is needed.
   */
  get (keyReference: string, publicKeyOnly: boolean): Promise<Buffer | PrivateKey | PublicKey>;

  /**
   * Saves the specified key to the key store using
   * the key reference.
   * @param keyReference Reference for the key being saved.
   * @param key being saved to the key store.
   */
  save (keyReference: string, key: Buffer | PrivateKey | PublicKey): Promise<void>;

  /**
   * Lists all key references with their corresponding key ids
   */
  list (): Promise<{ [name: string]: string }>;
}

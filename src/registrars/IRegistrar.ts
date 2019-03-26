/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

import Identifier from '../Identifier';
import IdentifierDocument from '../IdentifierDocument';

/**
 * Interface defining methods and properties to
 * be implemented by specific registration methods.
 */
export default interface IRegistrar {
  /**
   * Registers the identifier document on the ledger
   * returning the identifier generated by the registrar.
   * @param identifierDocument to register.
   * @param keyReference Reference to the identifier for the signing key.
   */
  register (identifierDocument: IdentifierDocument, keyReference: string): Promise<Identifier>;

  /**
   * Uses the specified input for generating
   * an identifier reference, but does not register
   * with a ledger.
   * @param input generating the identifier.
   */
  generateIdentifier (input: any): Promise<Identifier>;
}

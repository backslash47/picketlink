/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.picketlink.identity.federation.core.saml.v2.factories;

import org.picketlink.common.PicketLinkLogger;
import org.picketlink.common.PicketLinkLoggerFactory;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.identity.federation.core.saml.v2.common.IDGenerator;
import org.picketlink.identity.federation.core.saml.v2.holders.IDPInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.holders.IssuerInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.holders.SPInfoHolder;
import org.picketlink.identity.federation.core.saml.v2.util.XMLTimeUtil;
import org.picketlink.identity.federation.saml.v2.assertion.AssertionType;
import org.picketlink.identity.federation.saml.v2.assertion.ConditionsType;
import org.picketlink.identity.federation.saml.v2.assertion.EncryptedAssertionType;
import org.picketlink.identity.federation.saml.v2.assertion.NameIDType;
import org.picketlink.identity.federation.saml.v2.assertion.StatementAbstractType;
import org.picketlink.identity.federation.saml.v2.assertion.SubjectConfirmationDataType;
import org.picketlink.identity.federation.saml.v2.assertion.SubjectConfirmationType;
import org.picketlink.identity.federation.saml.v2.assertion.SubjectType;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType.RTChoiceType;
import org.picketlink.identity.federation.saml.v2.protocol.StatusCodeType;
import org.picketlink.identity.federation.saml.v2.protocol.StatusType;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;
import java.util.List;

/**
 * Factory for the SAML v2 Authn Response
 *
 * @author Anil.Saldhana@redhat.com
 * @since Dec 9, 2008
 */
public class JBossSAMLAuthnResponseFactory {
    
    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();
    
    /**
     * Create a StatusType given the status code uri
     *
     * @param statusCodeURI
     * @return
     */
    public static StatusType createStatusType(String statusCodeURI) {
        StatusCodeType sct = new StatusCodeType();
        sct.setValue(URI.create(statusCodeURI));

        StatusType statusType = new StatusType();
        statusType.setStatusCode(sct);
        return statusType;
    }

    /**
     * Create a ResponseType
     *
     * @param ID id of the response
     * @param sp holder with the information about the Service Provider
     * @param idp holder with the information on the Identity Provider
     * @param issuerInfo holder with information on the issuer
     * @return
     * @throws ConfigurationException
     */
    public static ResponseType createResponseType(String ID, SPInfoHolder sp, IDPInfoHolder idp, IssuerInfoHolder issuerInfo)
            throws ConfigurationException {
        String responseDestinationURI = sp.getResponseDestinationURI();

        XMLGregorianCalendar issueInstant = XMLTimeUtil.getIssueInstant();

        // Create an assertion
        String id = IDGenerator.create("ID_");

        // Create assertion -> subject
        SubjectType subjectType = new SubjectType();

        // subject -> nameid
        NameIDType nameIDType = new NameIDType();
        nameIDType.setFormat(URI.create(idp.getNameIDFormat()));
        nameIDType.setValue(idp.getNameIDFormatValue());

        SubjectType.STSubType subType = new SubjectType.STSubType();
        subType.addBaseID(nameIDType);
        subjectType.setSubType(subType);

        SubjectConfirmationType subjectConfirmation = new SubjectConfirmationType();
        subjectConfirmation.setMethod(idp.getSubjectConfirmationMethod());

        SubjectConfirmationDataType subjectConfirmationData = new SubjectConfirmationDataType();
        subjectConfirmationData.setInResponseTo(sp.getRequestID());
        subjectConfirmationData.setRecipient(responseDestinationURI);
        //subjectConfirmationData.setNotBefore(issueInstant);
        subjectConfirmationData.setNotOnOrAfter(issueInstant);

        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);

        subjectType.addConfirmation(subjectConfirmation);

        AssertionType assertionType = SAMLAssertionFactory.createAssertion(id, nameIDType, issueInstant, (ConditionsType) null,
                subjectType, (List<StatementAbstractType>) null);

        ResponseType responseType = createResponseType(ID, issuerInfo, assertionType);
        // InResponseTo ID
        responseType.setInResponseTo(sp.getRequestID());
        // Destination
        responseType.setDestination(responseDestinationURI);

        return responseType;
    }

    /**
     * Create a Response Type
     *
     * @param ID
     * @param issuerInfo
     * @param assertionType
     * @return
     * @throws ConfigurationException
     */
    public static ResponseType createResponseType(String ID, IssuerInfoHolder issuerInfo, AssertionType assertionType)
            throws ConfigurationException {
        XMLGregorianCalendar issueInstant = XMLTimeUtil.getIssueInstant();
        ResponseType responseType = new ResponseType(ID, issueInstant);

        // Issuer
        NameIDType issuer = issuerInfo.getIssuer();
        responseType.setIssuer(issuer);

        // Status
        String statusCode = issuerInfo.getStatusCode();
        if (statusCode == null)
            throw logger.issuerInfoMissingStatusCodeError();

        responseType.setStatus(createStatusType(statusCode));

        responseType.addAssertion(new RTChoiceType(assertionType));
        return responseType;
    }

    /**
     * Create a Response Type
     *
     * @param ID
     * @param issuerInfo
     * @param encryptedAssertion a DOM {@link Element} that represents an encrypted assertion
     * @return
     * @throws ConfigurationException
     */
    public static ResponseType createResponseType(String ID, IssuerInfoHolder issuerInfo, Element encryptedAssertion)
            throws ConfigurationException {
        ResponseType responseType = new ResponseType(ID, XMLTimeUtil.getIssueInstant());

        // Issuer
        NameIDType issuer = issuerInfo.getIssuer();
        responseType.setIssuer(issuer);

        // Status
        String statusCode = issuerInfo.getStatusCode();
        if (statusCode == null)
            throw logger.issuerInfoMissingStatusCodeError();

        responseType.setStatus(createStatusType(statusCode));

        responseType.addAssertion(new RTChoiceType(new EncryptedAssertionType(encryptedAssertion)));
        return responseType;
    }
}
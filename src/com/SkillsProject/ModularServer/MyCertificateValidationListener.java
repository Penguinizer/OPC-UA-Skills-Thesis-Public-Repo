/* The Certificate Validation Listener is used as provided from the Prosys OPC UA SDK tutorial.
 * It is a required component yet not within the scope of the project. As such the tutorial version was used.
 * 
 * Credit to Prosys.
 */
package com.SkillsProject.ModularServer;

import java.security.cert.CertificateParsingException;
import java.util.EnumSet;

import com.prosysopc.ua.stack.cert.CertificateCheck;
import com.prosysopc.ua.stack.cert.DefaultCertificateValidatorListener;
import com.prosysopc.ua.stack.cert.ValidationResult;
import com.prosysopc.ua.stack.core.ApplicationDescription;
import com.prosysopc.ua.stack.transport.security.Cert;
import com.prosysopc.ua.stack.utils.CertificateUtils;

import simpleserver.SimpleServerMain;


/**
 * A sample implementation of a DefaultCertificateValidatorListener.
 */
public class MyCertificateValidationListener implements DefaultCertificateValidatorListener {

  @Override
  public ValidationResult onValidate(Cert certificate, ApplicationDescription applicationDescription,
      EnumSet<CertificateCheck> passedChecks) {
    try {
    	SimpleServerMain
          .println(applicationDescription + ", " + CertificateUtils.getApplicationUriOfCertificate(certificate));
    } catch (CertificateParsingException e1) {
      throw new RuntimeException(e1);
    }

    /*
     * Checks that all compulsory certificate checks passed. NOTE! The CertificateCheck.Uri is
     * included in this list. This might prevent some client applications from connecting, if e.g.
     * their hostname would change domain. This check is needed for passing the compliance tests
     * provided by the OPC Foundation. But if you need to allow such client to connect, comment this
     * and instead use the commented implementation below.
     */
    if (passedChecks.containsAll(CertificateCheck.COMPULSORY)) {
      return ValidationResult.AcceptPermanently;
    } else {
      return ValidationResult.Reject;
    }

    // // Do not mind about URI...
    // if (passedChecks
    // .containsAll(EnumSet.of(CertificateCheck.Trusted, CertificateCheck.Validity,
    // CertificateCheck.Signature))) {
    // if (!passedChecks.contains(CertificateCheck.Uri)) {
    // try {
    // SampleConsoleServer.println("Client's ApplicationURI (" +
    // applicationDescription.getApplicationUri()
    // + ") does not match the one in certificate: "
    // + CertificateUtils.getApplicationUriOfCertificate(certificate));
    // } catch (CertificateParsingException e) {
    // throw new RuntimeException(e);
    // }
    // }
    // return ValidationResult.AcceptPermanently;
    // }
    // return ValidationResult.Reject;
  }

}

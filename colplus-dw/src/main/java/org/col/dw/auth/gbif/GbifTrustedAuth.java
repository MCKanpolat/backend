package org.col.dw.auth.gbif;

import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import org.col.dw.auth.AuthConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GBIF authentication scheme is modelled after the Amazon scheme on how to sign REST http requests
 * using a private key. It uses the standard Http Authorization Header to transport the following information:
 * Authorization: GBIF applicationKey:Signature
 *
 * <br/>
 * The header starts with the authentication scheme (GBIF), followed by the plain applicationKey (the public key)
 * and a unique signature for the very request which is generated using a fixed set of request attributes
 * which are then encrypted by a standard HMAC-SHA1 algorithm.
 *
 * <br/>
 * A POST request with a GBIF header would look like this:
 *
 * <pre>
 * POST /dataset HTTP/1.1
 * Host: johnsmith.s3.amazonaws.com
 * Date: Mon, 26 Mar 2007 19:37:58 +0000
 * x-gbif-user: trobertson
 * Content-MD5: LiFThEP4Pj2TODQXa/oFPg==
 * Authorization: GBIF gbif.portal:frJIUN8DYpKDtOLCwo//yllqDzg=
 * </pre>
 *
 * When signing an http request in addition to the Authentication header some additional custom headers are added
 * which are used to sign and digest the message.
 * <br/>
 * x-gbif-user is added to transport a proxied user in which the application is acting.
 * <br/>
 * Content-MD5 is added if a body entity exists.
 * See Concent-MD5 header specs: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
 */
public class GbifTrustedAuth {

  private static final Logger LOG = LoggerFactory.getLogger(GbifTrustedAuth.class);

  private static final String ALGORITHM = "HmacSHA1";
  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String GBIF_SCHEME = "GBIF";
  public static final String HEADER_GBIF_USER = "x-gbif-user";
  public static final String HEADER_ORIGINAL_REQUEST_URL = "x-url";
  private static final char NEWLINE = '\n';

  private final String appKey = "col.app";
  private final String appSecret;

  public GbifTrustedAuth(AuthConfiguration cfg) {
    
    this.appSecret = Preconditions.checkNotNull(cfg.gbifSecret, "To sign requests a GBIF secret for col.app is required");
    LOG.info("Using GBIF app {}", appKey);
  }

  /**
   * Build the string to be signed for a client request by extracting header information from the request.
   */
  private static String buildStringToSign(ClientRequestContext request) {
    StringBuilder sb = new StringBuilder();

    sb.append(request.getMethod());
    sb.append(NEWLINE);
    sb.append(getCanonicalizedPath(request.getUri()));

    appendHeader(sb, request.getHeaders(), HEADER_CONTENT_TYPE, false);
    appendHeader(sb, request.getHeaders(), HEADER_GBIF_USER, true);

    return sb.toString();
  }

  private static void appendHeader(StringBuilder sb, MultivaluedMap<String, ?> request, String header, boolean caseSensitive) {
    if (request.containsKey(header)) {
      sb.append(NEWLINE);
      if (caseSensitive) {
        sb.append(request.getFirst(header));
      } else {
        sb.append(request.getFirst(header).toString().toLowerCase());
      }
    }
  }

  /**
   * @return an absolute uri of the resource path alone, excluding host, scheme and query parameters
   */
  private static String getCanonicalizedPath(URI uri) {
    return uri.normalize().getPath();
  }

  private static String buildAuthHeader(String applicationKey, String signature) {
    return GBIF_SCHEME + " " + applicationKey + ':' + signature;
  }

  /**
   * Generates a Base64 encoded HMAC-SHA1 signature of the passed in string with the given secret key.
   * See Message Authentication Code specs http://tools.ietf.org/html/rfc2104
   * @param stringToSign the string to be signed
   * @param secretKey the secret key to use in the
   */
  private String buildSignature(String stringToSign, String secretKey) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      SecretKeySpec secret = new SecretKeySpec(secretKey.getBytes(Charset.forName("UTF8")), ALGORITHM);
      mac.init(secret);
      byte[] digest = mac.doFinal(stringToSign.getBytes());
  
      return BaseEncoding.base64().encode(digest);
  
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Cant find " + ALGORITHM + " message digester", e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException("Invalid secret key " + secretKey, e);
    }
  }

  /**
   * Signs a request by adding a Authorization header.
   */
  public void signRequest(String username, ClientRequestContext request) {
    // first add custom GBIF headers so we can use them to build the string to sign

    // the proxied username
    request.getHeaders().putSingle(HEADER_GBIF_USER, username);

    // the canonical path header
    request.getHeaders().putSingle(HEADER_ORIGINAL_REQUEST_URL, getCanonicalizedPath(request.getUri()));

    // build the unique string to sign
    final String stringToSign = buildStringToSign(request);

    // sign
    String signature = buildSignature(stringToSign, appSecret);

    // build authorization header string
    String header = buildAuthHeader(appKey, signature);
    // add authorization header
    LOG.debug("Adding authentication header to request {} for proxied user {} : {}", request.getUri(), username, header);
    request.getHeaders().putSingle(HEADER_AUTHORIZATION, header);
  }

}
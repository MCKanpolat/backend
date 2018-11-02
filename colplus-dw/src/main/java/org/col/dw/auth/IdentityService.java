package org.col.dw.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.col.api.model.ColUser;
import org.col.db.mapper.UserMapper;
import org.col.dw.auth.gbif.GbifTrustedAuth;
import org.col.dw.auth.gbif.HttpGbifAuthFilter;
import org.gbif.api.vocabulary.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identity service that delegates authentication to the BASIC scheme using the
 * GBIF id service via http.
 * It keeps a local copy of users and therefore needs access to Postgres.
 *
 * A SqlSessionFactory and an HttpClient MUST be set before the service is used.
 */
public class IdentityService {
  private static final Logger LOG = LoggerFactory.getLogger(IdentityService.class);
  private static final String SETTING_COUNTRY = "country";
  private static final String SYS_SETTING_ORCID = "auth.orcid.id";
  
  private final AuthConfiguration cfg;
  private SqlSessionFactory sqlSessionFactory;
  private ConcurrentHashMap<Integer, ColUser> cache;
  private final URI loginUri;
  private final URI userUri;
  private CloseableHttpClient http;
  
  public IdentityService(AuthConfiguration cfg) {
    this.cfg = cfg;
    try {
      loginUri = new URI(cfg.gbifApi).resolve("user/login");
      userUri = new URI(cfg.gbifApi).resolve("admin/user/");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    LOG.info("Accessing GBIF user accounts at {}", cfg.gbifApi);
    
    //TODO: replace with Chronicle Map and only cache the main webservice, not admin!
    this.cache = new ConcurrentHashMap<>();
  }
  
  /**
   * Wires up the mybatis sqlfactory to be used.
   */
  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }
  
  public void setClient(CloseableHttpClient http) {
    this.http = http;
  }

  public ColUser get(Integer key) {
    if (cache.containsKey(key)) {
      return cache.get(key);
    }
    // try to load from DB - if its not there the user has never logged in before and sth is wrong
    try (SqlSession session = sqlSessionFactory.openSession()) {
      ColUser user = session.getMapper(UserMapper.class).get(key);
      if (user == null) {
        throw new IllegalArgumentException("ColUser " + key + " does not exist");
      }
      return cache(user);
    }
  }

  private ColUser cache(ColUser user) {
    cache.put(user.getKey(), user);
    return user;
  }
  
  @VisibleForTesting
  Optional<ColUser> authenticate(String username, String password) {
    ColUser user = authenticateGBIF(username, password);
    if (user != null) {
      if (false) {
        // TODO: GBIF authentication does not provide us with the full user, we need to look it up again
        user = getFullGbifUser(username);
      }
      user.getRoles().add(ColUser.Role.USER);
      user.setLastLogin(LocalDateTime.now());
      // insert/update coluser in postgres with updated login date
      try (SqlSession session = sqlSessionFactory.openSession(true)) {
        UserMapper mapper = session.getMapper(UserMapper.class);
        // try to update user, create new one otherwise
        if (mapper.update(user) < 1) {
          LOG.info("Creating new CoL user {} {}", user.getUsername(), user.getKey());
          mapper.create(user);
          user.setCreated(LocalDateTime.now());
        }
      }
      return Optional.of(cache(user));
    }
    return Optional.empty();
  }
  
  /**
   * Checks if Basic Auth against GBIF API is working.
   * We avoid the native httpclient Basic authentication which uses ASCII to encode the password
   * into Base64 octets. But GBIF requires ISO_8859_1! See:
   *  https://github.com/gbif/registry/issues/67
   *  https://stackoverflow.com/questions/7242316/what-encoding-should-i-use-for-http-basic-authentication
   *  https://tools.ietf.org/html/rfc7617#section-2.1
   */
  @VisibleForTesting
  ColUser authenticateGBIF(String username, String password) {
    HttpGet get = new HttpGet(loginUri);
    get.addHeader("Authorization", basicAuthHeader(username, password));
    try (CloseableHttpResponse resp = http.execute(get)){
      LOG.debug("GBIF authentication response for {}: {}", username, resp);
      if (resp.getStatusLine().getStatusCode() == 200) {
        return fromJson("");
      }
    } catch (Exception e) {
      LOG.error("GBIF BasicAuth error", e);
    }
    return null;
  }
  
  @VisibleForTesting
  static String basicAuthHeader(String username, String password) {
    String cred = username + ":" + password;
    String base64 = BaseEncoding.base64().encode(cred.getBytes(StandardCharsets.ISO_8859_1));
    return "Basic " + base64;
  }
  
  @VisibleForTesting
  ColUser getFullGbifUser(String username) {
    try {
      new HttpGbifAuthFilter(new GbifTrustedAuth(cfg));
      HttpGet get = new HttpGet(userUri.resolve(username));
      CloseableHttpResponse resp = http.execute(get);

    } catch (Exception e) {
      LOG.info("Failed to retrieve GBIF user {}", username, e);
    }
    return null;
  }
  
  private ColUser fromJson(String json) {
    ColUser user = new ColUser();
    return user;
  }
  
  private static ColUser.Role fromGbifRole(UserRole gbif) {
    switch (gbif) {
      case USER: return ColUser.Role.USER;
      case REGISTRY_ADMIN: return ColUser.Role.ADMIN;
      case REGISTRY_EDITOR: return ColUser.Role.EDITOR;
    }
    return null;
  }
}
package life.catalogue.dw.jersey.exception;

import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.exceptions.PersistenceException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static life.catalogue.dw.jersey.exception.JsonExceptionMapperBase.jsonErrorResponse;

/**
 * Checks PersistenceExceptions for certain known conditions that are not server errors.
 * 1) see if they are caused by missing dataset partitions which are http 404
 * 2) check for violated unique constraints which are considered 400.
 */
@Provider
public class PersistenceExceptionMapper extends LoggingExceptionMapper<PersistenceException> {
  private static final Logger LOG = LoggerFactory.getLogger(PersistenceExceptionMapper.class);
  
  private final static Pattern RELATION = Pattern.compile("relation \"[a-z_]+_([0-9]+)\" does not exist");
  private final static Pattern UNIQUE = Pattern.compile("unique constraint \"([a-z]+)_");

  @Override
  public Response toResponse(PersistenceException e) {
    if (e.getCause() != null) {
      Matcher m = RELATION.matcher(e.getCause().getMessage());
      if (m.find()) {
        return jsonErrorResponse(Response.Status.NOT_FOUND, "Dataset " + Integer.parseInt(m.group(1)) + " does not exist", e.getMessage());
      }

      m = UNIQUE.matcher(e.getCause().getMessage());
      if (m.find()) {
        String entity = StringUtils.capitalize(m.group(1));
        return jsonErrorResponse(Response.Status.BAD_REQUEST, entity + " already exists", e.getMessage());
      }

      if (e.getCause() instanceof PSQLException) {
        PSQLException pe = (PSQLException) e.getCause();
        // All PgSql Error codes starting with 23 are constraint violations.
        // https://www.postgresql.org/docs/11/errcodes-appendix.html
        if (pe.getSQLState() != null && pe.getSQLState().startsWith("23")) {
          LOG.warn("Postgres constraint violation", pe);
          return jsonErrorResponse(Response.Status.BAD_REQUEST, "Database constraint violation", e.getMessage());
        }
      }
    }
    
    return super.toResponse(e);
  }
  
}

package life.catalogue.resources;

import io.dropwizard.auth.Auth;
import life.catalogue.api.exception.NotFoundException;
import life.catalogue.api.model.DSID;
import life.catalogue.api.model.DSIDValue;
import life.catalogue.api.model.Dataset;
import life.catalogue.api.model.User;
import life.catalogue.db.mapper.DatasetPatchMapper;
import life.catalogue.dw.auth.Roles;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Editorial decision patching the metadata of a source dataset.
 * The integer id value of the underlying DSID<Integer> refers to the source dataset key.
 */
@Path("/dataset/{key}/patch")
@Produces(MediaType.APPLICATION_JSON)
@SuppressWarnings("static-method")
@Consumes(MediaType.APPLICATION_JSON)
public class DatasetPatchResource {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetPatchResource.class);


  @GET
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  public List<Dataset> list(@PathParam("key") int datasetKey, @Context SqlSession session) {
    List<Dataset> patches = new ArrayList<>();
    session.getMapper(DatasetPatchMapper.class).processDataset(datasetKey).forEach(patches::add);
    return patches;
  }

  /**
   * @return the primary key of the object. Together with the CreatedResponseFilter will return a 201 location
   */
  @POST
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  public DSID<Integer> create(@PathParam("key") int datasetKey, Dataset obj, @Auth User user, @Context SqlSession session) {
    obj.applyUser(user);
    session.getMapper(DatasetPatchMapper.class).create(datasetKey, obj);
    session.commit();
    return DSID.of(datasetKey, obj.getKey());
  }

  /**
   * Gets entity by its key and throws NotFoundException if not existing
   */
  @GET
  @Path("{id}")
  public Dataset get(@PathParam("key") int datasetKey, @PathParam("id") Integer id, @Context SqlSession session) {
    return session.getMapper(DatasetPatchMapper.class).get(datasetKey, id);
  }

  @PUT
  @Path("{id}")
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  public void update(@PathParam("key") int datasetKey, @PathParam("id") Integer id, @Valid Dataset obj, @Auth User user, @Context SqlSession session) {
    obj.setKey(id);
    obj.applyUser(user);
    int i = session.getMapper(DatasetPatchMapper.class).update(datasetKey, obj);
    if (i == 0) {
      // not existing yet, lets allow to create it via PUT
      obj.setKey(id);
      create(datasetKey, obj, user, session);
    }
    session.commit();
  }

  @DELETE
  @Path("{id}")
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  public void delete(@PathParam("key") int datasetKey, @PathParam("id") Integer id, @Auth User user, @Context SqlSession session) {
    int i = session.getMapper(DatasetPatchMapper.class).delete(datasetKey, id);
    if (i == 0) {
      throw NotFoundException.notFound("DatasetPatch", new DSIDValue<>(datasetKey, id));
    }
    session.commit();
  }

}

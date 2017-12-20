package org.col.db.mapper;

import org.apache.ibatis.annotations.Param;
import org.col.api.Page;
import org.col.api.Taxon;

import java.util.List;

/**
 *
 */
public interface TaxonMapper {

  Integer lookupKey(@Param("id") String id, @Param("datasetKey") int datasetKey);

  int count(@Param("datasetKey") Integer datasetKey);

  List<Taxon> list(@Param("datasetKey") Integer datasetKey, @Param("page") Page page);

  Taxon get(@Param("key") int key);

  List<Taxon> classification(@Param("key") int key);

  List<Taxon> children(@Param("key") int key, @Param("page") Page page);

  void create(Taxon taxon);

}


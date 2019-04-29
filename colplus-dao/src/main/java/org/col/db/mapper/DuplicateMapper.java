package org.col.db.mapper;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Param;
import org.col.api.model.Duplicate;
import org.col.api.model.Page;
import org.col.api.vocab.MatchingMode;
import org.col.api.vocab.TaxonomicStatus;
import org.gbif.nameparser.api.Rank;

public interface DuplicateMapper {
  
  List<Duplicate.Mybatis> duplicates(@Param("mode") MatchingMode mode,
                             @Param("minSize") Integer minSize,
                             @Param("datasetKey") int datasetKey,
                             @Param("sectorKey") Integer sectorKey,
                             @Param("rank") Rank rank,
                             @Param("status") Set<TaxonomicStatus> status,
                             @Param("authorshipDifferent") Boolean authorshipDifferent,
                             @Param("parentDifferent") Boolean parentDifferent,
                             @Param("withDecision") Boolean withDecision,
                             @Param("page") Page page);
  
  /**
   * @param ids usage ids to return usage decisions for
   */
  List<Duplicate.UsageDecision> usagesByIds(@Param("datasetKey") int datasetKey, @Param("ids") List<String> ids);
  
}

package bg.autohouse.data.repositories;

import bg.autohouse.data.models.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {

  @Query(
      "SELECT DISTINCT m "
          + "FROM Model m "
          + "LEFT JOIN FETCH m.trims t "
          + "WHERE m.maker.id = :makerId")
  Stream<Model> findAllByMakerId(Long makerId);

  @Query("SELECT DISTINCT m FROM Model m LEFT JOIN FETCH m.trims t WHERE m.id = :modelId")
  Stream<Model> findAllByModelId(Long modelId);

  @Query(
      value =
          "SELECT DISTINCT t.year "
              + "FROM auto_models as m "
              + "LEFT JOIN auto_trims as t on  m.id = t.model_id "
              + "WHERE m.id = :modelId order by t.year desc",
      nativeQuery = true)
  Stream<Object[]> findAllYearsByModelId(Long modelId);

  @Query(
      "SELECT DISTINCT m "
          + "FROM Model m "
          + "LEFT JOIN FETCH m.trims t "
          + "WHERE m.id = :modelId AND t.year = :year")
  Stream<Model> findAllByModelIdAndModelYear(Long modelId, Integer year);

  boolean existsByNameAndMakerId(String name, Long makerId);

  @Query(
      "SELECT DISTINCT m "
          + "FROM Model m "
          + "LEFT JOIN FETCH m.maker mk "
          + "LEFT JOIN FETCH m.trims t "
          + "WHERE m.name = :name AND mk.name = :makerName")
  Optional<Model> findByNameAndMakerName(String name, String makerName);
}

package bg.autohouse.data.models.offer;

import bg.autohouse.data.models.BaseUuidEntity;
import bg.autohouse.data.models.EntityConstants;
import bg.autohouse.data.models.enums.*;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import javax.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = EntityConstants.VEHICLES,
    indexes = {
      @Index(
          name = "idx_" + EntityConstants.VEHICLES + "maker_model",
          columnList = "maker_name, model_name"),
      @Index(name = "idx_" + EntityConstants.VEHICLES + "_maker_name", columnList = "maker_name"),
      @Index(name = "idx_" + EntityConstants.VEHICLES + "_model_name", columnList = "model_name"),
      @Index(name = "idx_" + EntityConstants.VEHICLES + "_body_style", columnList = "body_style"),
    })
public class Vehicle extends BaseUuidEntity {

  private static final long serialVersionUID = -797198687403481966L;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "offer_id",
      nullable = false,
      referencedColumnName = "id",
      foreignKey = @ForeignKey(name = "fk_vehicle_offer_id"))
  private Offer offer;

  @Column(name = "maker_name", nullable = false)
  private String makerName;

  @Column(name = "maker_id", nullable = false)
  private Long makerId;

  @Column(name = "model_name", nullable = false)
  private String modelName;

  @Column(name = "model_id", nullable = false)
  private Long modelId;

  @Column(name = "trim")
  private String trim;

  @Column(name = "year", nullable = false)
  private Integer year;

  @Column(name = "mileage")
  private Integer mileage;

  @Column(name = "doors")
  private Integer doors;

  @Column(name = "state", nullable = false)
  @Enumerated(EnumType.STRING)
  private State state;

  @Column(name = "body_style", nullable = false)
  @Enumerated(EnumType.STRING)
  private BodyStyle bodyStyle;

  @Column(name = "transmission", nullable = false)
  @Enumerated(EnumType.STRING)
  private Transmission transmission;

  @Column(name = "drive")
  @Enumerated(EnumType.STRING)
  private Drive drive;

  @Column(name = "color")
  @Enumerated(EnumType.STRING)
  private Color color;

  @Column(name = "fuel_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private FuelType fuelType;

  @ElementCollection(fetch = FetchType.LAZY, targetClass = Feature.class)
  @JoinTable(
      name = EntityConstants.PREFIX + "vehicles_features",
      joinColumns =
          @JoinColumn(
              name = "vehicle_id",
              referencedColumnName = "id",
              foreignKey = @ForeignKey(name = EntityConstants.PREFIX + "fk_feature_vehicle_id")))
  @Column(name = "feature")
  @Enumerated(value = EnumType.STRING)
  private List<Feature> features = new ArrayList<>();

  @Column(name = "has_accident")
  private boolean hasAccident = false;
}

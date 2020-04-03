package bg.autohouse.data.models;

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
@Table(name = EntityConstants.LOCATIONS)
public class Location extends BaseLongEntity {

  private static final long serialVersionUID = -2377398736911388136L;

  @Column(name = "city", nullable = false)
  private String city;

  @Column(name = "city_region", nullable = false)
  private String cityRegion;

  @Column(name = "country", nullable = false)
  private String country;

  @Column(name = "postal_code")
  private String postalCode;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "latitude", column = @Column(nullable = true)),
    @AttributeOverride(name = "longitude", column = @Column(nullable = true))
  })
  private GeoLocation geo;

  @Column(name = "maps_url")
  private String mapsUrl;
}

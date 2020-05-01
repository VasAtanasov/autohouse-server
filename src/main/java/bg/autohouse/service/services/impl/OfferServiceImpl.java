package bg.autohouse.service.services.impl;

import static org.springframework.data.jpa.domain.Specification.where;

import bg.autohouse.data.models.Filter;
import bg.autohouse.data.models.account.Account;
import bg.autohouse.data.models.geo.Location;
import bg.autohouse.data.models.media.MediaFunction;
import bg.autohouse.data.models.offer.Offer;
import bg.autohouse.data.models.offer.Vehicle;
import bg.autohouse.data.repositories.AccountRepository;
import bg.autohouse.data.repositories.LocationRepository;
import bg.autohouse.data.repositories.OfferRepository;
import bg.autohouse.data.specifications.OfferSpecifications;
import bg.autohouse.errors.AccountNotFoundException;
import bg.autohouse.errors.LocationNotFoundException;
import bg.autohouse.service.models.offer.OfferServiceModel;
import bg.autohouse.service.services.MediaFileService;
import bg.autohouse.service.services.OfferService;
import bg.autohouse.util.Assert;
import bg.autohouse.util.ImageResizer;
import bg.autohouse.util.ModelMapperWrapper;
import bg.autohouse.web.enums.RestMessage;
import bg.autohouse.web.models.request.FilterRequest;
import bg.autohouse.web.models.request.offer.OfferCreateRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class OfferServiceImpl implements OfferService {

  private final OfferRepository offerRepository;
  private final ModelMapperWrapper modelMapper;
  private final LocationRepository locationRepository;
  private final AccountRepository accountRepository;
  private final MediaFileService mediaFileService;
  private final ImageResizer imageResizer;

  @Override
  @Transactional(readOnly = true)
  public List<OfferServiceModel> getTopOffers() {
    Sort sort = Sort.by("createdAt").descending();
    Pageable pageable = PageRequest.of(0, 20, sort);
    List<OfferServiceModel> topOffers =
        offerRepository
            .findLatestOffers(pageable)
            .map(offer -> modelMapper.map(offer, OfferServiceModel.class))
            .collect(Collectors.toUnmodifiableList());
    return topOffers;
  }

  // TODO refactor filter request to add maker and models names
  @Override
  @Transactional(readOnly = true)
  public Page<OfferServiceModel> searchOffers(FilterRequest filterRequest, Pageable pageable) {
    Objects.requireNonNull(filterRequest);
    Filter filter = modelMapper.map(filterRequest, Filter.class);
    if (Assert.has(filterRequest.getMakerId())) {
      filter.setMakerId(filterRequest.getMakerId());
      if (Assert.has(filterRequest.getModelId())) {
        filter.setModelId(filterRequest.getModelId());
      }
    }
    return offerRepository
        .findAll(where(OfferSpecifications.getOffersByFilter(filter)), pageable)
        .map(offer -> modelMapper.map(offer, OfferServiceModel.class));
  }

  @Override
  // TODO validate numbers
  @Transactional(rollbackFor = IOException.class)
  public OfferServiceModel createOffer(OfferCreateRequest request, UUID creatorId)
      throws IOException {
    Assert.notNull(creatorId, "User id is required");
    Assert.notNull(request, "Offer model is required");
    Account account =
        accountRepository.findByUserId(creatorId).orElseThrow(AccountNotFoundException::new);
    Location location =
        locationRepository
            .findById(request.getLocationId())
            .orElseThrow(LocationNotFoundException::new);
    Vehicle vehicle = modelMapper.map(request.getVehicle(), Vehicle.class);
    Assert.notNull(vehicle.getBodyStyle(), RestMessage.INVALID_BODY_STYLE.name());
    Assert.notNull(vehicle.getColor(), RestMessage.INVALID_COLOR.name());
    Assert.notNull(vehicle.getDrive(), RestMessage.INVALID_DRIVE.name());
    Assert.notNull(vehicle.getFuelType(), RestMessage.INVALID_FUEL_TYPE.name());
    Assert.notNull(vehicle.getState(), RestMessage.INVALID_VEHICLE_STATE.name());
    Assert.notNull(vehicle.getTransmission(), RestMessage.INVALID_TRANSMISSION.name());
    Assert.notNulls(vehicle.getFeatures(), RestMessage.INVALID_FEATURE.name());
    request.setVehicle(null);
    Offer offer = modelMapper.map(request, Offer.class);
    offer.setLocation(location);
    offer.setAccount(account);
    offer = offerRepository.save(offer);
    offer.setVehicle(vehicle);
    vehicle.setOffer(offer);
    for (MultipartFile file : request.getImages()) {
      byte[] byteArray = imageResizer.toJpgDownscaleToSize(file.getInputStream());
      String fileName =
          generateFileName(
              file.getContentType(),
              Integer.toString(vehicle.getYear()),
              vehicle.getMakerName(),
              vehicle.getModelName(),
              "pic",
              Long.toString(System.currentTimeMillis()));
      String fileKey = generateFileKey(offer.getId(), fileName);
      mediaFileService.storeFile(
          byteArray,
          fileKey,
          MediaFunction.OFFER_IMAGE,
          file.getContentType(),
          file.getOriginalFilename(),
          offer.getId());
    }
    return modelMapper.map(offer, OfferServiceModel.class);
  }

  private String generateFileName(String contentType, String... params) {
    String ext =
        contentType.replace("image/", "").equals("jpeg")
            ? "jpg"
            : contentType.replace("image/", "");
    return String.join("_", params).toLowerCase().replaceAll("\\s+", "_") + "." + ext;
  }

  private String generateFileKey(UUID referenceId, String fileName) {
    LocalDate now = LocalDate.now();
    return String.join(
        "/",
        "offer-images-folder",
        Integer.toString(now.getYear()),
        String.format("%02d", now.getMonthValue()),
        String.format("%02d", now.getDayOfMonth()),
        referenceId.toString(),
        fileName);
  }
}

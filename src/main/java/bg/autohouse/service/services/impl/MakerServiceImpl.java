package bg.autohouse.service.services.impl;

import bg.autohouse.data.models.Maker;
import bg.autohouse.data.models.Model;
import bg.autohouse.data.repositories.MakerRepository;
import bg.autohouse.data.repositories.ModelRepository;
import bg.autohouse.errors.MakerNotFoundException;
import bg.autohouse.errors.ResourceAlreadyExistsException;
import bg.autohouse.service.models.MakerModelServiceModel;
import bg.autohouse.service.models.MakerServiceModel;
import bg.autohouse.service.models.ModelServiceModel;
import bg.autohouse.service.models.ModelTrimsServicesMode;
import bg.autohouse.service.services.MakerService;
import bg.autohouse.util.Assert;
import bg.autohouse.util.ModelMapperWrapper;
import bg.autohouse.web.enums.RestMessage;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MakerServiceImpl implements MakerService {
  // TODO replace exception messages
  private final MakerRepository makerRepository;
  private final ModelRepository modelRepository;
  private final ModelMapperWrapper modelMapper;

  @Override
  @Transactional(readOnly = true)
  public MakerModelServiceModel getOne(Long id) {
    return makerRepository
        .findById(id)
        .map(maker -> modelMapper.map(maker, MakerModelServiceModel.class))
        .orElseThrow(MakerNotFoundException::new);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MakerServiceModel> getAllMakers() {
    return modelMapper.mapAll(makerRepository.findAll(), MakerServiceModel.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MakerModelServiceModel> getAllMakerWithModels() {
    return modelMapper.mapAll(makerRepository.findAllWithModels(), MakerModelServiceModel.class);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isMaker(Long id) {
    return makerRepository.existsById(id);
  }

  @Override
  @Transactional
  public MakerServiceModel addModelToMaker(
      @Nonnull Long makerId, @Nonnull ModelServiceModel modelServiceModel) {
    Assert.notNull(modelServiceModel, "Model ise required");
    modelServiceModel.setId(null); // modelMapper maps id to model
    Maker maker = makerRepository.findById(makerId).orElseThrow(MakerNotFoundException::new);
    boolean modelExists =
        modelRepository.existsByNameAndMakerId(modelServiceModel.getName(), makerId);
    if (modelExists) {
      throw new ResourceAlreadyExistsException(RestMessage.MODEL_ALREADY_EXISTS);
    }
    Model model = modelMapper.map(modelServiceModel, Model.class);
    model.setMaker(maker);
    maker.getModels().add(model);
    modelRepository.save(model);
    makerRepository.save(maker);
    return modelMapper.map(maker, MakerServiceModel.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ModelTrimsServicesMode> getMakerModelsTrims(Long makerId) {
    return modelMapper.mapAll(
        modelRepository.findAllByMakerId(makerId), ModelTrimsServicesMode.class);
  }

  @Override
  @Transactional
  public MakerServiceModel createMaker(@Nonnull MakerServiceModel makerServiceModel) {
    if (makerRepository.existsByName(makerServiceModel.getName())) {
      throw new ResourceAlreadyExistsException(RestMessage.MAKER_ALREADY_EXISTS);
    }
    Maker maker = modelMapper.map(makerServiceModel, Maker.class);
    makerRepository.save(maker);
    return modelMapper.map(maker, MakerServiceModel.class);
  }
}

package bg.autohouse.service.services.impl;

import bg.autohouse.data.models.Maker;
import bg.autohouse.data.models.Model;
import bg.autohouse.data.repositories.MakerRepository;
import bg.autohouse.data.repositories.ModelRepository;
import bg.autohouse.errors.ExceptionsMessages;
import bg.autohouse.errors.MakerNotFoundException;
import bg.autohouse.errors.ResourceAlreadyExistsException;
import bg.autohouse.service.models.MakerServiceModel;
import bg.autohouse.service.models.ModelServiceModel;
import bg.autohouse.service.services.MakerService;
import bg.autohouse.util.ModelMapperWrapper;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MakerServiceImpl implements MakerService {

  private final MakerRepository makerRepository;
  private final ModelRepository modelRepository;
  private final ModelMapperWrapper modelMapper;

  @Override
  @Transactional(readOnly = true)
  public MakerServiceModel getOne(Long id) {
    return makerRepository
        .findById(id)
        .map(maker -> modelMapper.map(maker, MakerServiceModel.class))
        .orElseThrow(MakerNotFoundException::new);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MakerServiceModel> getAllMakers() {
    return modelMapper.mapAll(makerRepository.findAll(), MakerServiceModel.class);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isMaker(Long id) {
    return makerRepository.existsById(id);
  }

  @Override
  public MakerServiceModel addModelToMaker(
      @Nonnull Long makerId, ModelServiceModel modelServiceModel) {

    Maker maker = makerRepository.findById(makerId).orElseThrow(MakerNotFoundException::new);

    boolean modelExists =
        modelRepository.existsByNameAndMakerId(modelServiceModel.getName(), makerId);

    if (modelExists) {
      throw new ResourceAlreadyExistsException(ExceptionsMessages.MODEL_WITH_NAME_EXISTS);
    }

    Model model = modelMapper.map(modelServiceModel, Model.class);
    model.setMaker(maker);
    modelRepository.save(model);

    return modelMapper.map(maker, MakerServiceModel.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ModelServiceModel> getModelsForMaker(Long makerId) {
    return modelMapper.mapAll(modelRepository.findAllByMakerId(makerId), ModelServiceModel.class);
  }

  @Override
  public MakerServiceModel createMaker(@Nonnull MakerServiceModel makerServiceModel) {
    boolean makerExists = makerRepository.existsByName(makerServiceModel.getName());

    if (makerExists) {
      throw new ResourceAlreadyExistsException(ExceptionsMessages.MAKER_WITH_NAME_EXISTS);
    }

    Maker maker = modelMapper.map(makerServiceModel, Maker.class);
    makerRepository.save(maker);

    return modelMapper.map(maker, MakerServiceModel.class);
  }
}

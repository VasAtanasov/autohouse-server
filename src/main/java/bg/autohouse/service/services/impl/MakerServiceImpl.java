package bg.autohouse.service.services.impl;

import bg.autohouse.data.models.Maker;
import bg.autohouse.data.models.Model;
import bg.autohouse.data.models.Trim;
import bg.autohouse.data.repositories.MakerRepository;
import bg.autohouse.data.repositories.ModelRepository;
import bg.autohouse.errors.MakerNotFoundException;
import bg.autohouse.errors.NotFoundException;
import bg.autohouse.errors.ResourceAlreadyExistsException;
import bg.autohouse.service.models.MakerModelServiceModel;
import bg.autohouse.service.models.MakerServiceModel;
import bg.autohouse.service.models.ModelServiceModel;
import bg.autohouse.service.models.ModelTrimsServicesModel;
import bg.autohouse.service.services.MakerService;
import bg.autohouse.util.Assert;
import bg.autohouse.util.ModelMapperWrapper;
import bg.autohouse.web.enums.RestMessage;
import bg.autohouse.web.models.request.MakerModelsTrimsCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MakerServiceImpl implements MakerService {

  @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:50}")
  private int batchSize;

  @PersistenceContext private EntityManager em;

  private final MakerRepository makerRepository;
  private final ModelRepository modelRepository;
  private final ModelMapperWrapper modelMapper;

  @Override
  @Transactional(readOnly = true)
  public MakerModelServiceModel getOne(Long id) {
    return makerRepository
        .findMakerById(id)
        .map(maker -> modelMapper.map(maker, MakerModelServiceModel.class))
        .orElseThrow(MakerNotFoundException::new);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MakerModelServiceModel> getAllMakerWithModels() {
    return modelMapper.mapAll(makerRepository.findAllWithModels(), MakerModelServiceModel.class);
  }

  @Override
  @Transactional
  public MakerServiceModel addModelToMaker(Long makerId, ModelServiceModel modelServiceModel) {
    Assert.notNull(modelServiceModel, "Model is required");
    modelServiceModel.setId(null); // modelMapper maps id to model
    Maker maker = makerRepository.findMakerById(makerId).orElseThrow(MakerNotFoundException::new);
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
  public List<ModelTrimsServicesModel> getMakerModelsTrims(Long makerId) {
    return modelMapper.mapAll(
        modelRepository.findAllByMakerId(makerId), ModelTrimsServicesModel.class);
  }

  @Override
  @Transactional
  public MakerServiceModel createMaker(MakerServiceModel makerServiceModel) {
    if (makerRepository.existsByName(makerServiceModel.getName())) {
      throw new ResourceAlreadyExistsException(RestMessage.MAKER_ALREADY_EXISTS);
    }
    Maker maker = modelMapper.map(makerServiceModel, Maker.class);
    makerRepository.save(maker);
    return modelMapper.map(maker, MakerServiceModel.class);
  }

  @Override
  @Transactional(readOnly = true)
  public ModelTrimsServicesModel getModel(String makerName, String modelName) {
    Model model =
        modelRepository
            .findByNameAndMakerName(modelName, makerName)
            .orElseThrow(NotFoundException::new);
    return modelMapper.map(model, ModelTrimsServicesModel.class);
  }

  @Override
  @Transactional
  public int createMakerModelsTrimsBulk(List<MakerModelsTrimsCreateRequest> makersRequest) {
    for (MakerModelsTrimsCreateRequest makerRequest : makersRequest) {
      Maker maker = modelMapper.map(makerRequest, Maker.class);
      List<Model> models = maker.getModels();
      maker.setModels(new ArrayList<>());
      em.persist(maker);
      Long makerId = maker.getId();
      for (Model model : models) {
        List<Trim> trims = model.getTrims();
        model.setTrims(new ArrayList<>());
        model.setMaker(maker);
        maker.getModels().add(model);
        em.persist(model);
        Long modelId = model.getId();
        for (int j = 0; j < trims.size(); j++) {
          Trim trim = trims.get(j);
          trim.setModel(model);
          model.getTrims().add(trim);
          if (j % batchSize == 0) {
            em.flush();
            em.clear();
            maker = em.getReference(Maker.class, makerId);
            model = em.getReference(Model.class, modelId);
          }
        }
      }
    }
    return (int) makerRepository.count();
  }
}

package com.sushil.audittrail.service;

import com.sushil.audittrail.dto.AuditTrailDto.*;
import com.sushil.audittrail.entity.ModelRegistry;
import com.sushil.audittrail.entity.ModelRegistry.ModelStatus;
import com.sushil.audittrail.repository.ModelRegistryRepository;
import com.sushil.exception.AppExceptions.DuplicateResourceException;
import com.sushil.exception.AppExceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModelRegistryService {

    private final ModelRegistryRepository repo;

    @Transactional
    public CreateModelResponse create(CreateModelRequest req) {
        if (repo.findByModelId(req.modelId()).isPresent())
            throw new DuplicateResourceException("Model already registered: " + req.modelId());

        ModelRegistry saved = repo.save(ModelRegistry.builder()
                .modelId(req.modelId())
                .modelVersion(req.modelVersion())
                .modelType(req.modelType())
                .provider(req.provider())
                .build());

        return CreateModelResponse.builder().id(saved.getId()).status("SUCCESS").build();
    }

    @Transactional(readOnly = true)
    public ModelResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ModelResponse> search(String modelId, String provider, String modelType, Pageable pageable) {
        return repo.search(modelId, provider, modelType, pageable).map(this::toResponse);
    }

    @Transactional
    public void setStatus(String id, ModelStatus status) {
        ModelRegistry m = findOrThrow(id);
        m.setStatus(status);
        repo.save(m);
    }

    private ModelRegistry findOrThrow(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Model", id));
    }

    private ModelResponse toResponse(ModelRegistry m) {
        return ModelResponse.builder()
                .id(m.getId())
                .modelId(m.getModelId())
                .modelVersion(m.getModelVersion())
                .modelType(m.getModelType())
                .provider(m.getProvider())
                .status(m.getStatus().name())
                .build();
    }
}

package com.evolution.dropfiledaemon.controller.api;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.evolution.dropfile.common.dto.ApiQuickShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiQuickShareLsResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiQuickShareFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quickshare")
public class ApiQuickShareRestController {

    private final ApiQuickShareFacade facade;

    @PostMapping("/add")
    public ApiQuickShareLsResponseDTO add(@RequestBody ApiQuickShareAddRequestDTO requestDTO) {
        return facade.add(requestDTO);
    }

    @GetMapping("/ls")
    public List<ApiQuickShareLsResponseDTO> getAll() {
        return facade.ls();
    }

    @PostMapping("/show")
    public ApiQuickShareLsResponseDTO show(@RequestBody CriteriaEnvelope idCriteriaEnvelope) {
        return facade.show(idCriteriaEnvelope);
    }

    @DeleteMapping("/rm")
    public ApiBatchOperationResult removeByCriteria(@RequestBody Collection<CriteriaEnvelope> idCriteriaEnvelopes) {
        return facade.removeByCriteria(idCriteriaEnvelopes);
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        facade.removeAll();
    }
}

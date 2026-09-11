package com.evolution.dropfiledaemon.controller.api;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiBatchOperationResult;
import com.evolution.dropfile.common.dto.ApiConnectionsShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiConnectionsShareFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/connections/share")
public class ApiConnectionsShareRestController {

    private static final String HEADER_TIMEOUT = "X-Timeout-Ms";

    private final ApiConnectionsShareFacade apiFacade;

    @PostMapping("/add")
    public ApiConnectionsShareLsResponseDTO add(@RequestBody ApiConnectionsShareAddRequestDTO requestDTO, @RequestHeader(value = HEADER_TIMEOUT, required = false) Long timeout) throws Exception {
        return apiFacade.add(requestDTO, timeout);
    }

    @GetMapping("/ls")
    public List<ApiConnectionsShareLsResponseDTO> ls() {
        return apiFacade.ls();
    }

    @DeleteMapping("/rm")
    public ApiBatchOperationResult rm(@RequestBody Collection<CriteriaEnvelope> shareFileIdCriteriaEnvelopes) {
        return apiFacade.rm(shareFileIdCriteriaEnvelopes);
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        apiFacade.rmAll();
    }
}

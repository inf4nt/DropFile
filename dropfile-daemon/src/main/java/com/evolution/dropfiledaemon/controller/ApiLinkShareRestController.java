package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfile.common.dto.ApiLinkShareAddRequestDTO;
import com.evolution.dropfile.common.dto.ApiLinkShareLsResponseDTO;
import com.evolution.dropfiledaemon.facade.ApiLinkShareFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/link-share")
public class ApiLinkShareRestController {

    private final ApiLinkShareFacade apiLinkShareFacade;

    @PostMapping("/add")
    public ApiLinkShareLsResponseDTO add(@RequestBody ApiLinkShareAddRequestDTO requestDTO) {
        return apiLinkShareFacade.add(requestDTO);
    }

    @GetMapping("/ls")
    public List<ApiLinkShareLsResponseDTO> getAll() {
        return apiLinkShareFacade.ls();
    }

    @DeleteMapping("/rm/{id}")
    public void removeById(@PathVariable String id) {
        apiLinkShareFacade.removeByKeyStartWith(id);
    }

    @DeleteMapping("/rm-all")
    public void rmAll() {
        apiLinkShareFacade.removeAll();
    }
}

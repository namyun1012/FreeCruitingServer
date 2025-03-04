package com.project.freecruting.service;

import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PartyService {
    private final PartyRepository partyRepository;


}

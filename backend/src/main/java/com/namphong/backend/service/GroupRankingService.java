package com.namphong.backend.service;

import com.namphong.backend.repository.GroupRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupRankingService {
    private final GroupRankingRepository groupRankingRepository;
}

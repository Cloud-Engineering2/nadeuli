package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.repository.RegionRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {



    private final RegionRepository regionRepository;


}

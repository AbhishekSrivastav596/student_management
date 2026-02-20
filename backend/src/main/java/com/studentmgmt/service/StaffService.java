package com.studentmgmt.service;

import com.studentmgmt.dto.StaffDto;
import com.studentmgmt.entity.Staff;
import com.studentmgmt.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    public Page<StaffDto> getAll(String search, Boolean active, Pageable pageable) {
        Page<Staff> page;
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch && active != null) {
            page = staffRepository.searchByActive(search, active, pageable);
        } else if (hasSearch) {
            page = staffRepository.search(search, pageable);
        } else if (active != null) {
            page = staffRepository.findByActive(active, pageable);
        } else {
            page = staffRepository.findAll(pageable);
        }
        return page.map(this::toDto);
    }

    public StaffDto toggleActive(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found with id: " + id));
        staff.setActive(!staff.isActive());
        return toDto(staffRepository.save(staff));
    }

    public StaffDto getById(Long id) {
        return toDto(staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found with id: " + id)));
    }

    public StaffDto create(StaffDto dto) {
        Staff staff = toEntity(dto);
        return toDto(staffRepository.save(staff));
    }

    public StaffDto update(Long id, StaffDto dto) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found with id: " + id));

        staff.setFirstName(dto.getFirstName());
        staff.setLastName(dto.getLastName());
        staff.setEmail(dto.getEmail());
        staff.setPhone(dto.getPhone());
        staff.setDepartment(dto.getDepartment());
        staff.setPosition(dto.getPosition());
        staff.setJoinDate(dto.getJoinDate());
        staff.setSalary(dto.getSalary());
        staff.setQualification(dto.getQualification());
        staff.setAddress(dto.getAddress());

        return toDto(staffRepository.save(staff));
    }

    public void delete(Long id) {
        if (!staffRepository.existsById(id)) {
            throw new RuntimeException("Staff not found with id: " + id);
        }
        staffRepository.deleteById(id);
    }

    @Transactional
    public void bulkDelete(List<Long> ids) {
        staffRepository.deleteAllByIdInBatch(ids);
    }

    @Transactional
    public void bulkSetActive(List<Long> ids, boolean active) {
        List<Staff> staffList = staffRepository.findAllByIdIn(ids);
        staffList.forEach(s -> s.setActive(active));
        staffRepository.saveAll(staffList);
    }

    private StaffDto toDto(Staff staff) {
        StaffDto dto = new StaffDto();
        dto.setId(staff.getId());
        dto.setFirstName(staff.getFirstName());
        dto.setLastName(staff.getLastName());
        dto.setEmail(staff.getEmail());
        dto.setPhone(staff.getPhone());
        dto.setDepartment(staff.getDepartment());
        dto.setPosition(staff.getPosition());
        dto.setJoinDate(staff.getJoinDate());
        dto.setActive(staff.isActive());
        dto.setSalary(staff.getSalary());
        dto.setQualification(staff.getQualification());
        dto.setAddress(staff.getAddress());
        return dto;
    }

    private Staff toEntity(StaffDto dto) {
        return Staff.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .department(dto.getDepartment())
                .position(dto.getPosition())
                .joinDate(dto.getJoinDate())
                .active(dto.isActive())
                .salary(dto.getSalary())
                .qualification(dto.getQualification())
                .address(dto.getAddress())
                .build();
    }
}

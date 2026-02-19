package com.studentmgmt.service;

import com.studentmgmt.dto.StudentDto;
import com.studentmgmt.entity.Student;
import com.studentmgmt.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public Page<StudentDto> getAll(String search, Boolean active, Pageable pageable) {
        Page<Student> page;
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch && active != null) {
            page = studentRepository.searchByActive(search, active, pageable);
        } else if (hasSearch) {
            page = studentRepository.search(search, pageable);
        } else if (active != null) {
            page = studentRepository.findByActive(active, pageable);
        } else {
            page = studentRepository.findAll(pageable);
        }
        return page.map(this::toDto);
    }

    public StudentDto toggleActive(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        student.setActive(!student.isActive());
        return toDto(studentRepository.save(student));
    }

    public StudentDto getById(Long id) {
        return toDto(studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id)));
    }

    public StudentDto create(StudentDto dto) {
        Student student = toEntity(dto);
        return toDto(studentRepository.save(student));
    }

    public StudentDto update(Long id, StudentDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setEmail(dto.getEmail());
        student.setPhone(dto.getPhone());
        student.setStudentClass(dto.getStudentClass());
        student.setSection(dto.getSection());
        student.setEnrollmentDate(dto.getEnrollmentDate());

        return toDto(studentRepository.save(student));
    }

    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new RuntimeException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }

    @Transactional
    public void bulkDelete(List<Long> ids) {
        studentRepository.deleteAllByIdInBatch(ids);
    }

    @Transactional
    public void bulkSetActive(List<Long> ids, boolean active) {
        List<Student> students = studentRepository.findAllByIdIn(ids);
        students.forEach(s -> s.setActive(active));
        studentRepository.saveAll(students);
    }

    public List<StudentDto> getByIds(List<Long> ids) {
        return studentRepository.findAllByIdIn(ids).stream()
                .map(this::toDto)
                .toList();
    }

    private StudentDto toDto(Student student) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEmail(student.getEmail());
        dto.setPhone(student.getPhone());
        dto.setStudentClass(student.getStudentClass());
        dto.setSection(student.getSection());
        dto.setEnrollmentDate(student.getEnrollmentDate());
        dto.setActive(student.isActive());
        return dto;
    }

    private Student toEntity(StudentDto dto) {
        return Student.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .studentClass(dto.getStudentClass())
                .section(dto.getSection())
                .enrollmentDate(dto.getEnrollmentDate())
                .active(dto.isActive())
                .build();
    }
}

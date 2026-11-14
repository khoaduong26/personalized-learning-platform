package com.tlcn.backend.academic.service;
import com.tlcn.backend.academic.entity.Subject;
import com.tlcn.backend.academic.repository.SubjectRepository;
import com.tlcn.backend.academic.repository.TopicRepository;
import com.tlcn.backend.academic.dto.SubjectRequest;
import com.tlcn.backend.academic.dto.SubjectResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class SubjectService {
    private final SubjectRepository repository; private final TopicRepository topics;
    public SubjectService(SubjectRepository repository, TopicRepository topics) { this.repository = repository; this.topics = topics; }
    @Transactional(readOnly = true) public List<SubjectResponse> findAll() { return repository.findAllByOrderByNameAsc().stream().map(s -> new SubjectResponse(s.getId(),s.getName(),(int)topics.countBySubjectId(s.getId()))).toList(); }
    @Transactional(readOnly = true) public SubjectResponse get(Long id) { Subject s = requireSubject(id); return new SubjectResponse(s.getId(),s.getName(),(int)topics.countBySubjectId(id)); }
    @Transactional public SubjectResponse create(SubjectRequest request) {
        String name = request.name().strip(); ensureUnique(name, null);
        Subject s = repository.save(new Subject(name));
        return new SubjectResponse(s.getId(),s.getName(),0);
    }
    @Transactional public SubjectResponse update(Long id, SubjectRequest request) {
        Subject s = requireSubject(id); String name = request.name().strip(); ensureUnique(name,id); s.setName(name);
        s = repository.save(s); return new SubjectResponse(s.getId(),s.getName(),(int)topics.countBySubjectId(id));
    }
    @Transactional public void delete(Long id) {
        Subject s = requireSubject(id); if (topics.existsBySubjectId(id)) throw new AcademicConflictException("Hãy xóa các chủ đề trước khi xóa môn học."); repository.delete(s);
    }
    private Subject requireSubject(Long id) { return repository.findById(id).orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy môn học.")); }
    private void ensureUnique(String name, Long exceptId) { if (repository.existsByNameIgnoreCase(name) && (exceptId == null || !repository.findById(exceptId).map(s -> s.getName().equalsIgnoreCase(name)).orElse(false))) throw new AcademicConflictException("Tên môn học đã tồn tại."); }
}

package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.dto.ApprovalStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.Approval;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.NameAndId;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ApprovalRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final CourtService courtService;
    private final ServiceCentreService serviceCentreService;
    private final UserService userService;

    public List<ApprovalStatus> getAllApprovalStatuses() {
        Map<SubjectKey, Approval> approvalsBySubject = approvalRepository.findAll().stream()
            .collect(java.util.stream.Collectors.toMap(
                approval -> new SubjectKey(approval.getSubjectId(), approval.getSubjectType()),
                Function.identity(),
                (existing, replacement) -> existing
            ));

        return Stream.concat(
            toApprovalStatuses(courtService.getAllCourtNameAndIds(), SubjectType.COURT, approvalsBySubject),
            toApprovalStatuses(
                serviceCentreService.getAllServiceCentreNameAndIds(),
                SubjectType.SERVICE_CENTRE,
                approvalsBySubject
            )
        ).toList();
    }

    public Approval createApproval(Approval approval) {
        validateApprovalReferences(approval);

        approval.setId(
            approvalRepository.findBySubjectIdAndSubjectType(approval.getSubjectId(), approval.getSubjectType())
                .map(Approval::getId)
                .orElse(null)
        );

        return approvalRepository.save(approval);
    }

    @Transactional
    public void deleteApproval(UUID approvalId) {
        if (!approvalRepository.existsById(approvalId)) {
            throw new NotFoundException("Approval not found, ID: " + approvalId);
        }

        approvalRepository.deleteById(approvalId);
    }

    private void validateApprovalReferences(Approval approval) {
        userService.getUserById(approval.getUserId());

        if (SubjectType.COURT.equals(approval.getSubjectType())) {
            courtService.getCourtById(approval.getSubjectId());
        } else if (SubjectType.SERVICE_CENTRE.equals(approval.getSubjectType())) {
            serviceCentreService.getServiceCentreById(approval.getSubjectId());
        }
    }

    private Stream<ApprovalStatus> toApprovalStatuses(List<NameAndId> subjects, SubjectType subjectType,
                                                      Map<SubjectKey, Approval> approvalsBySubject) {
        return subjects.stream()
            .map(subject -> toApprovalStatus(subject, subjectType, approvalsBySubject));
    }

    private ApprovalStatus toApprovalStatus(NameAndId subject, SubjectType subjectType,
                                            Map<SubjectKey, Approval> approvalsBySubject) {
        Approval approval = approvalsBySubject.get(new SubjectKey(subject.id(), subjectType));

        return new ApprovalStatus(
            subject.id(),
            subjectType,
            subject.name(),
            approval != null,
            approval == null ? null : approval.getId(),
            approval == null ? null : approval.getUserId(),
            approval == null ? null : approval.getUser(),
            approval == null ? null : approval.getLastUpdatedAt()
        );
    }

    private record SubjectKey(UUID subjectId, SubjectType subjectType) {
    }
}

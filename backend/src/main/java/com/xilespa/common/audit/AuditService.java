package com.xilespa.common.audit;

import com.xilespa.security.UserPrincipal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Ghi nhật ký thay đổi (FR-SYS-01, BR-18). Mọi module ghi nhật ký bằng {@link #record}, gọi trong
 * service của mình, CÙNG transaction với thay đổi gốc. Xem nhật ký: {@code module/system}.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    /** Tên trường (không phân biệt hoa thường) bị che giá trị trước khi lưu. */
    private static final Set<String> SENSITIVE_FIELDS =
            Set.of(
                    "password",
                    "passwordhash",
                    "password_hash",
                    "token",
                    "tokenhash",
                    "token_hash",
                    "accesstoken",
                    "refreshtoken",
                    "rawrefreshtoken",
                    "secret");

    static final String MASK = "***";

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Ghi một dòng nhật ký. Bắt buộc gọi trong transaction đang mở của nghiệp vụ ({@link
     * Propagation#MANDATORY}): nghiệp vụ rollback thì nhật ký cũng rollback; gọi ngoài transaction
     * thì báo lỗi ngay.
     *
     * @param entityType tên bảng của đối tượng, ví dụ {@code service}
     * @param before giá trị trước khi đổi (DTO, record hoặc Map); {@code null} khi tạo mới
     * @param after giá trị sau khi đổi; các trường nhạy cảm (mật khẩu, token) tự được che
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            AuditAction action, String entityType, Object entityId, Object before, Object after) {
        AuditLog entry =
                new AuditLog(
                        clock.instant(),
                        currentActorId(),
                        action,
                        entityType,
                        entityId == null ? null : String.valueOf(entityId),
                        toMaskedJson(before),
                        toMaskedJson(after));
        auditLogRepository.save(entry);
    }

    private Long currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.id();
        }
        return null;
    }

    private String toMaskedJson(Object value) {
        if (value == null) {
            return null;
        }
        JsonNode tree = objectMapper.valueToTree(value);
        mask(tree);
        return objectMapper.writeValueAsString(tree);
    }

    private static void mask(JsonNode node) {
        if (node instanceof ObjectNode object) {
            List<String> names = new ArrayList<>();
            for (Map.Entry<String, JsonNode> property : object.properties()) {
                names.add(property.getKey());
            }
            for (String name : names) {
                if (SENSITIVE_FIELDS.contains(name.toLowerCase())) {
                    object.put(name, MASK);
                } else {
                    mask(object.get(name));
                }
            }
        } else if (node != null && node.isArray()) {
            node.forEach(AuditService::mask);
        }
    }
}

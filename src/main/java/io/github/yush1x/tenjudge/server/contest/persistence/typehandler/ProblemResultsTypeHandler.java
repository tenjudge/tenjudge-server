package io.github.yush1x.tenjudge.server.contest.persistence.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yush1x.tenjudge.server.contest.dto.ProblemResultDTO;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * 将ContestParticipant.problemResults字段（Map<Long, ProblemResultDTO>）序列化为 JSON 存储到数据库中，并在读取时反序列化回 Map。
 */

@MappedTypes(Map.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class ProblemResultsTypeHandler extends BaseTypeHandler<Map<Long, ProblemResultDTO>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final TypeReference<Map<Long, ProblemResultDTO>> TYPE_REFERENCE = new TypeReference<>() {
    };

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<Long, ProblemResultDTO> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            // 比赛题目结果以 jsonb 落库，便于后续直接按参赛者维度整体读取和更新
            ps.setObject(i, OBJECT_MAPPER.writeValueAsString(parameter), Types.OTHER);
        } catch (Exception e) {
            throw new SQLException("problem_results 序列化失败", e);
        }
    }

    @Override
    public Map<Long, ProblemResultDTO> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return deserialize(rs.getString(columnName));
    }

    @Override
    public Map<Long, ProblemResultDTO> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return deserialize(rs.getString(columnIndex));
    }

    @Override
    public Map<Long, ProblemResultDTO> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return deserialize(cs.getString(columnIndex));
    }

    private Map<Long, ProblemResultDTO> deserialize(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, TYPE_REFERENCE);
        } catch (Exception e) {
            throw new SQLException("problem_results 反序列化失败", e);
        }
    }
}

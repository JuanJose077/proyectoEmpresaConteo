package com.pasantia.proyecto.roadsafety.repository;

import com.pasantia.proyecto.roadsafety.domain.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate localJdbc;

    public UserRepository(@Qualifier("localJdbcTemplate") JdbcTemplate localJdbc) {
        this.localJdbc = localJdbc;
    }

    public Optional<User> findByEmail(String email) {
        String sql = """
            SELECT id, email, password, role, active, must_change_password, created_by, created_at
            FROM users
            WHERE email = ?
            LIMIT 1
        """;

        List<User> rows = localJdbc.query(sql, (rs, rowNum) ->
                new User(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getBoolean("active"),
                        rs.getBoolean("must_change_password"),
                        rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                )
        , email);

        return rows.stream().findFirst();
    }

    public Optional<User> findById(Long id) {
        String sql = """
            SELECT id, email, password, role, active, must_change_password, created_by, created_at
            FROM users
            WHERE id = ?
            LIMIT 1
        """;

        List<User> rows = localJdbc.query(sql, (rs, rowNum) ->
                new User(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getBoolean("active"),
                        rs.getBoolean("must_change_password"),
                        rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                )
        , id);

        return rows.stream().findFirst();
    }

    public List<User> findAll() {
        String sql = """
            SELECT id, email, password, role, active, must_change_password, created_by, created_at
            FROM users
            ORDER BY created_at DESC
        """;

        return localJdbc.query(sql, (rs, rowNum) ->
                new User(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getBoolean("active"),
                        rs.getBoolean("must_change_password"),
                        rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                )
        );
    }

    public long createUser(
            String email,
            String passwordHash,
            String role,
            boolean active,
            boolean mustChangePassword,
            Long createdBy
    ) {
        String sql = """
            INSERT INTO users (email, password, role, active, must_change_password, created_by)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        localJdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.setString(3, role);
            ps.setBoolean(4, active);
            ps.setBoolean(5, mustChangePassword);
            if (createdBy == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, createdBy);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    public int updatePassword(long id, String passwordHash, boolean mustChangePassword) {
        String sql = """
            UPDATE users
            SET password = ?, must_change_password = ?
            WHERE id = ?
        """;

        return localJdbc.update(sql, passwordHash, mustChangePassword, id);
    }

    public int deactivateUser(long id) {
        String sql = """
            UPDATE users
            SET active = 0
            WHERE id = ?
        """;

        return localJdbc.update(sql, id);
    }

    public int resetPassword(long id, String passwordHash) {
        String sql = """
            UPDATE users
            SET password = ?, must_change_password = 1, active = 1
            WHERE id = ?
        """;

        return localJdbc.update(sql, passwordHash, id);
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}

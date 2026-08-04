package com.kb.youngly.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterMapperXmlTest {

    private static final String RESOURCE =
            "com/kb/youngly/mapper/CharacterMapper.xml";
    private static final String NAMESPACE =
            "com.kb.youngly.mapper.CharacterMapper.";

    @Test
    @DisplayName("CharacterMapper XML이 파싱되고 모든 SQL이 등록된다")
    void characterMapperXml_registersAllStatements() throws Exception {
        Configuration configuration = loadConfiguration();

        assertTrue(configuration.hasMapper(CharacterMapper.class));
        assertTrue(configuration.hasStatement(NAMESPACE + "lockUserForUpdate"));
        assertTrue(configuration.hasStatement(NAMESPACE + "findUnownedCharacters"));
        assertTrue(configuration.hasStatement(NAMESPACE + "insertUserItem"));
        assertTrue(configuration.hasStatement(NAMESPACE + "findOwnedCharacters"));
    }

    @Test
    @DisplayName("동시 중복 방지와 미보유·최신순 조회 SQL이 포함되어 있다")
    void characterMapperXml_containsConcurrencyAndFilteringSql() throws Exception {
        Configuration configuration = loadConfiguration();

        String lockSql = sql(configuration, "lockUserForUpdate");
        String candidatesSql = sql(configuration, "findUnownedCharacters");
        String ownedSql = sql(configuration, "findOwnedCharacters");

        assertTrue(lockSql.contains("FOR UPDATE"));
        assertTrue(candidatesSql.contains("ITEM_CATEGORY = 'CHARACTER'"));
        assertTrue(candidatesSql.contains("NOT EXISTS"));
        assertTrue(ownedSql.contains("ORDER BY UI.CREATED_AT DESC, UI.USER_ITEM_ID DESC"));
    }

    private Configuration loadConfiguration() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        try (InputStream input = Resources.getResourceAsStream(RESOURCE)) {
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    input,
                    configuration,
                    RESOURCE,
                    configuration.getSqlFragments()
            );
            builder.parse();
        }
        return configuration;
    }

    private String sql(Configuration configuration, String statementId) {
        MappedStatement statement = configuration.getMappedStatement(NAMESPACE + statementId);
        return statement.getBoundSql(Map.of("userId", "test-user"))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase();
    }
}

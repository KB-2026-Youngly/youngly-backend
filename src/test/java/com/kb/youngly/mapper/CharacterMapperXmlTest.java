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
        assertTrue(configuration.hasStatement(NAMESPACE + "findOwnedCharacterForEquip"));
        assertTrue(configuration.hasStatement(NAMESPACE + "unequipOtherCharacters"));
        assertTrue(configuration.hasStatement(NAMESPACE + "equipCharacter"));
        assertTrue(configuration.hasStatement(NAMESPACE + "updateUserProfileImage"));
    }

    @Test
    @DisplayName("동시 중복 방지와 미보유·최신순 조회 SQL이 포함되어 있다")
    void characterMapperXml_containsConcurrencyAndFilteringSql() throws Exception {
        Configuration configuration = loadConfiguration();

        String lockSql = sql(configuration, "lockUserForUpdate");
        String candidatesSql = sql(configuration, "findUnownedCharacters");
        String ownedSql = sql(configuration, "findOwnedCharacters");
        String ownedForEquipSql = sql(configuration, "findOwnedCharacterForEquip");
        String unequipSql = sql(configuration, "unequipOtherCharacters");
        String equipSql = sql(configuration, "equipCharacter");
        String profileImageSql = sql(configuration, "updateUserProfileImage");

        assertTrue(lockSql.contains("FOR UPDATE"));
        assertTrue(candidatesSql.contains("ITEM_CATEGORY = 'CHARACTER'"));
        assertTrue(candidatesSql.contains("NOT EXISTS"));
        assertTrue(ownedSql.contains("ORDER BY UI.CREATED_AT DESC, UI.USER_ITEM_ID DESC"));
        assertTrue(ownedForEquipSql.contains("UI.USER_ID = ?"));
        assertTrue(ownedForEquipSql.contains("CI.ITEM_CATEGORY = 'CHARACTER'"));
        assertTrue(unequipSql.contains("ITEM_ID <> ?"));
        assertTrue(unequipSql.contains("SET UI.IS_EQUIPPED = FALSE"));
        assertTrue(unequipSql.contains("CI.ITEM_CATEGORY IN"));
        assertTrue(unequipSql.contains("'ACC'"));
        assertTrue(equipSql.contains("SET IS_EQUIPPED = TRUE"));
        assertTrue(profileImageSql.contains("SET PROFILE_IMAGE_URL = ?"));
        assertTrue(profileImageSql.contains("WHERE USER_ID = ?"));
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
        return statement.getBoundSql(Map.of(
                        "userId", "test-user",
                        "characterId", 1L,
                        "imageUrl", "/characters/1.png"
                ))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase();
    }
}

package org.noorm.platform;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 27.03.14
 *         Time: 17:47
 */
public class Parameter {

    private String name;
    private JDBCType jdbcType;
    private String typeName;
    private String direction;

    public String getName() {
        return name;
    }

    public void setName(final String pName) {
        name = pName;
    }

    public JDBCType getJDBCType() {
        return jdbcType;
    }

    public void setJDBCType(final JDBCType pJDBCType) {
        jdbcType = pJDBCType;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(final String pTypeName) {
        typeName = pTypeName;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(final String pDirection) {
        direction = pDirection;
    }
}

package org.noorm.jdbc;

import java.util.ArrayList;
import java.util.List;

/**
 * For declared queries, paging and sorting may be required.
 * This class is user to specify and transport paging and filtering information to
 * the runtime engine.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@ext.secunet.com
 *         Date: 11.09.17
 *         Time: 14:38
 */
public class FilterExtension {

    /**
     * Paging is expected to get used for providing data for UI presentation, thus, any page size
     * (e.g. "count") larger than 1024 does not seem to be a reasonable choice.
     */
    public static final int UNLIMITED_COUNT = 1024;

    private int index = 0;
    private int count = UNLIMITED_COUNT;
    private List<SortCriteria> sortCriteria = new ArrayList<>();

    public int getIndex() {
        return index;
    }

    public void setIndex(final int pIndex) {
        index = pIndex;
    }

    public int getCount() {
        return count;
    }

    public void setCount(final int pCount) {
        count = pCount;
    }

    public List<SortCriteria> getSortCriteria() {
        return sortCriteria;
    }

    public void setSortCriteria(final List<SortCriteria> pSortCriteria) {
        sortCriteria = pSortCriteria;
    }

    public void addSortCriteria(final String pAttributeName) {
        sortCriteria.add(new SortCriteria(pAttributeName));
    }

    public void addSortCriteria(final String pAttributeName, final Direction pDirection) {
        sortCriteria.add(new SortCriteria(pAttributeName, pDirection));
    }

    /**
     * Inner class for the specification of sort criteria (column and direction)
     * Note the distinction between attribute name and column name. The latter denotes the database
     * column name, while the first denotes the Java name.
     */
    public static class SortCriteria implements Comparable<SortCriteria> {

        private String attributeName;
        private String columnName;
        private Direction direction = Direction.ASC;

        public SortCriteria(final String pAttributeName) {
            attributeName = pAttributeName;
        }

        public SortCriteria(final String pAttributeName, final Direction pDirecetion) {
            attributeName = pAttributeName;
            direction = pDirecetion;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public void setAttributeName(final String pAttributeName) {
            attributeName = pAttributeName;
        }

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(final String pColumnName) {
            columnName = pColumnName;
        }

        public Direction getDirection() {
            return direction;
        }

        public void setDirection(final Direction pDirection) {
            direction = pDirection;
        }

        /**
         * Compares this object with the specified object for order.  Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         *
         * @param other the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object
         * is less than, equal to, or greater than the specified object.
         * @throws NullPointerException if the specified object is null
         * @throws ClassCastException   if the specified object's type prevents it
         *                              from being compared to this object.
         */
        @Override
        public int compareTo(SortCriteria other) {
            return this.getAttributeName().compareTo(other.getAttributeName());
        }
    }

    public enum Direction {
        ASC,
        DESC;
    }
}

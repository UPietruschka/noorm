package org.noorm.jdbc;

import java.util.TreeSet;

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
    private TreeSet<SortCriteria> sortCriteria = new TreeSet<>();

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

    public TreeSet<SortCriteria> getSortCriteria() {
        return sortCriteria;
    }

    public void setSortCriteria(final TreeSet<SortCriteria> pSortCriteria) {
        sortCriteria = pSortCriteria;
    }

    public void addSortCriteria(final String pColumnName) {
        sortCriteria.add(new SortCriteria(pColumnName));
    }

    public void addSortCriteria(final String pColumnName, final Direction pDirection) {
        sortCriteria.add(new SortCriteria(pColumnName, pDirection));
    }

    /**
     * Inner class for the specification of sort criteria (column and direction)
     */
    public static class SortCriteria implements Comparable<SortCriteria> {

        private String columnName;
        private Direction direction = Direction.ASC;

        public SortCriteria(final String pColumnName) {
            columnName = pColumnName;
        }

        public SortCriteria(final String pColumnName, final Direction pDirecetion) {
            columnName = pColumnName;
            direction = pDirecetion;
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
            return this.getColumnName().compareTo(other.getColumnName());
        }
    }

    public enum Direction {
        ASC,
        DESC;
    }
}

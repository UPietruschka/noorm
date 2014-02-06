package org.noorm.jdbc;

import org.noorm.metadata.BeanMetaDataUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Pageable list to provide detached access to data from a data source.
 * Instead of backing the complete contents of the respective data objects,
 * this list only stores an array of Ids, assuming that each data object is
 * uniquely identified by an Id of type Long.
 * The list primarily overrides method subList to retrieve the required page
 * on demand, passing the Ids contained in the requested sublist to the
 * repository responsible for the data object type.
 * This mechanism does not only avoid re-issuing the complete initial query,
 * the list is based on, but also guarantees a consistent snapshot based on
 * the time the initial query was issued (at least, as long no data has been
 * deleted in between).
 * The PageableDataList has also a second backing store for the Beans themselves.
 * This second backing store, the "preFetchArray" is initially empty, but stores
 * the Beans retrieved by calls to subList(). This avoids multiple calls to the
 * same record.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public class PageableBeanList<T extends IBean> implements Serializable, List<T> {

	private static final long serialVersionUID = 5064051821172924938L;

	private static final int pageSize = 10;

	private static final String UNSUPPORTED_METHOD_MESSAGE =
			"Only methods subList(), size() and isEmpty() are supported. This list implementation is "
					+ "immutable and neither supports modifications nor single record access. Use method subList() "
					+ "to get the sub-list, resp. page of your choice. The ArrayList returned by subList contains "
					+ "the actually requested Bean objects (in contrast to the PageableBeanList, which contains "
					+ "objects of type Long).";

	/**
	 * Backing store for the full list of Ids for the detached data objects
	 */
	private List<Long> beanIds = new ArrayList<Long>();

	private final String plsqlIdListCallable;
	private final String refCursorName;
	private final String idListName;
	private final Class beanClass;

	// We must pre-instantiate the array for the given number of Ids, thus we cannot
	// define the array to be of type T.
	private final Object[] prefetchArray;

	private BeanTransformer<T> beanTransformer;

	public PageableBeanList(final Long[] pIDArray,
							final String pPLSQLIdListCallable,
							final String pRefCursorName,
							final String pIDListName,
							final Class pBeanClass) {

		beanIds = Arrays.asList(pIDArray);
		prefetchArray = new Object[beanIds.size()];
		plsqlIdListCallable = pPLSQLIdListCallable;
		refCursorName = pRefCursorName;
		idListName = pIDListName;
		beanClass = pBeanClass;
	}

	@Override
	public List<T> subList(final int fromIndex, final int toIndex) {

		List<Long> idSubList = beanIds.subList(fromIndex, toIndex);
		List<T> beanSubList = new ArrayList<T>();
		if (!idSubList.isEmpty()) {
			JDBCProcedureProcessor<T> statementProcessor = JDBCProcedureProcessor.getInstance();
			Long[] dataIdArray = idSubList.toArray(new Long[]{});
			final Map<String, Object> filterParameters = new HashMap<String, Object>();
			filterParameters.put(idListName, dataIdArray);
			beanSubList = statementProcessor.getBeanListFromPLSQL
					(plsqlIdListCallable, refCursorName, filterParameters, beanClass);
		}

		// Unfortunately, the query based on the ID list does not preserve the order of the
		// provided list. So we must re-order the retrieved bean-list in the order of the Ids.
		// The following code does not use the most efficient algorithm for sorting.
		// However, idSubList is not expected to be large, but typically in the range 10 - 100.
		List<T> orderedBeanSubList = new ArrayList<T>();
		for (Long id : idSubList) {
			for (T bean : beanSubList) {
				Long primaryKeyValue = BeanMetaDataUtil.getPrimaryKeyValue(bean);
				if (primaryKeyValue.equals(id)) {
					orderedBeanSubList.add(bean);
				}
			}
		}

		if (beanTransformer != null) {
			beanTransformer.preTransformAction(orderedBeanSubList);
			for (T bean : orderedBeanSubList) {
				beanTransformer.transformBean(bean);
			}
			beanTransformer.postTransformAction(orderedBeanSubList);
		}

		// Store the retrieved page in the prefetch array
		int index = fromIndex;
		for (T t : orderedBeanSubList) {
			prefetchArray[index++] = t;
		}

		return orderedBeanSubList;
	}

	public void addBeanTransformer(final BeanTransformer<T> pBeanTransformer) {
		beanTransformer = pBeanTransformer;
	}

	public BeanTransformer getBeanTransformer() {
		return beanTransformer;
	}

	public void removeBeanTransformer() {
		beanTransformer = null;
	}

	@Override
	public boolean add(final T o) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public boolean addAll(final Collection c) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public void add(final int index, final T element) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public boolean addAll(final int index, final Collection c) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public boolean remove(final Object o) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public T remove(final int index) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public boolean removeAll(final Collection c) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public boolean retainAll(final Collection c) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	/**
	 * Throws an UnsupportedOperationException.
	 * Could be implemented using a single round-trip to the data-source for
	 * a single data object, which is not the desired behaviour.
	 *
	 * @param pIndex position of the element.
	 * @return the element at the position given with index pIndex
	 */
	@Override
	public T get(final int pIndex) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public int size() {
		return beanIds.size();
	}

	@Override
	public boolean isEmpty() {
		return beanIds.isEmpty();
	}

	@Override
	public boolean contains(final Object o) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public Iterator<T> iterator() {
		return new PageableIterator<IBean>();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public IBean set(final int index, final IBean element) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public int indexOf(final Object o) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public int lastIndexOf(final Object o) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public ListIterator<T> listIterator() {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	@Override
	public ListIterator<T> listIterator(final int index) {
		throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
	}

	public static abstract class BeanTransformer<T> {

		public void preTransformAction(final List<T> beanList) {
		}

		public abstract void transformBean(final T bean);

		public void postTransformAction(final List<T> beanList) {
		}
	}

	class PageableIterator<T> implements Iterator {

		private int index = 0;

		@Override
		public boolean hasNext() {
			return index != beanIds.size();
		}

		@Override
		public T next() {
			if (prefetchArray[index] == null) {
				int endIndex = index + pageSize > beanIds.size() ? beanIds.size() : index + pageSize;
				subList(index, endIndex);
			}
			return (T) prefetchArray[index++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(UNSUPPORTED_METHOD_MESSAGE);
		}
	}
}

package org.basex.query.xquery.util;

import org.basex.query.xquery.XQException;
import org.basex.query.xquery.item.Item;
import org.basex.query.xquery.item.Seq;
import org.basex.query.xquery.iter.Iter;
import org.basex.query.xquery.iter.SeqIter;

/**
 * Simple Sequence Builder. In contrary to the {@link SeqBuilder},
 * sequences are not flattened.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class SeqBuilder {
  /** Item array. */
  public Item[] item;
  /** Number of entries. */
  public int size;

  /**
   * Constructor.
   */
  public SeqBuilder() {
    item = new Item[1];
  }

  /**
   * Constructor.
   * @param it initial iteration
   * @throws XQException evaluation exception
   */
  public SeqBuilder(final Iter it) throws XQException {
    item = new Item[1];
    add(it);
  }

  /**
   * Adds the items of an iterator.
   * @param it entry to be added
   * @throws XQException evaluation exception
   */
  public void add(final Iter it) throws XQException {
    Item i;
    while((i = it.next()) != null) add(i);
  }

  /**
   * Adds an atomic item.
   * Note that the item must be no sequence.
   * @param i entry to be added
   */
  public void add(final Item i) {
    if(size == item.length) resize();
    item[size++] = i;
  }

  /**
   * Resizes the sequence array.
   */
  private void resize() {
    final Item[] tmp = new Item[size << 1];
    System.arraycopy(item, 0, tmp, 0, size);
    item = tmp;
  }

  /**
   * Returns an atomic item or a sequence.
   * @return sequence
   */
  public Item finish() {
    return Seq.get(item, size);
  }

  /**
   * Returns an iterator.
   * @return sequence
   */
  public Iter iter() {
    return SeqIter.get(item, size);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + size + " items]";
  }
}

package data_structures;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class Hashtable<K extends Comparable<K>,V> implements DictionaryADT<K,V>{
	@SuppressWarnings("hiding")
	class DictionaryNode<K,V> implements Comparable<DictionaryNode<K,V>>{
		K key;
		V value;
		public DictionaryNode(K key, V value) {
			this.key = key;
			this.value = value;
		}
		
		@SuppressWarnings("unchecked")
		public int compareTo(DictionaryNode<K,V> o) { //will return 0, 1, or -1
			return ((Comparable<K>)o.key).compareTo(this.key);
		}
	}
	private int currentSize;
	private int tableSize;
	private long modCounter;
	private LinkedListDS<DictionaryNode<K,V>>[] hArray;
	
	@SuppressWarnings("unchecked")
	public Hashtable(int n) {
		//used this code from Rob Edwards' video (Hashes 13 Constructor for a chained hash)
		currentSize = 0;
		tableSize = n;	
		modCounter = 0;
		hArray = new LinkedListDS[tableSize];	//the hash array is initialized
		for (int i = 0; i < tableSize; i++) //for every index in our table, we create a linked list 
			hArray[i] = new LinkedListDS<DictionaryNode<K,V>>();
	}
	private int getHashVal(K key) {
		return (key.hashCode() & 0x7fffffff) % tableSize;  
	} 
	@SuppressWarnings("unused")
	private boolean contains(K key) {
		return hArray[getHashVal(key)].contains(new DictionaryNode<K,V>(key,null));
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean put(K key, V value) {
		@SuppressWarnings("unchecked")
		DictionaryNode<K,V> newNode = new DictionaryNode(key,value); //initialize node which takes in key value pairs
		boolean insertionSuccess = false;
		if (!hArray[getHashVal(key)].contains(newNode)) { //makes sure that a duplicate key cannot be inserted
			hArray[getHashVal(key)].addLast(newNode); //we add the node to the end
			insertionSuccess = true; 
			currentSize++;	//increment size 
			modCounter++;
		}
		return insertionSuccess;
	}

	@Override
	public boolean delete(K key) {
		if (isEmpty())	//we cannot remove a node if it's empty
			return false;
		boolean keyFound = false;
		@SuppressWarnings({ "rawtypes", "unchecked" })
		DictionaryNode<K,V> newNode = new DictionaryNode(key, null);	
		//taken from Rob Edward's video (Hashes 15 add() and remove() methods)
		for (DictionaryNode<K,V> dn: hArray[getHashVal(key)]) { //we iterate at the location where the key is located in the table
			if (newNode.key.compareTo(dn.key)== 0) { //we compare the keys and they are equal
				hArray[getHashVal(key)].remove(newNode); //remove key from the linked list
				keyFound = true; //we have found our key
				currentSize--;
				modCounter++;
				break;	//exit loop
			}
		}
		return keyFound;
	}

	@Override
	public V get(K key) { //taken from Rob Edward's video (Hashes 16 getValue())
		DictionaryNode<K,V> temp = hArray[getHashVal(key)].search(new DictionaryNode
				<K,V>(key, null)); //we search for the node where the key is located so we can retrieve its value
		if (temp != null)	
			return temp.value;
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public K getKey(V value) {
		if (isEmpty())
			return null;
		
		for (int i = 0; i < tableSize; i++) {
				LinkedListDS<DictionaryNode<K,V>> list = hArray[i]; //for each element, we get its linked list
			for (DictionaryNode<K,V> n: list) { //we compare the values in each linked list
				if (((Comparable<V>) n.value).compareTo(value) == 0) { //if the values are equal, we return the key within the node
					return n.key;
				}		
			}
		}
		
		return null;	//key not found
	}

	@Override
	public int size() {
		return currentSize;
	}

	@Override
	public boolean isFull() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return currentSize == 0;
	}

	@Override
	public void clear() {
		for (int i = 0; i < tableSize; i++) //we clear the linked lists for each element in our table
			hArray[i].makeEmpty();
		currentSize = 0;
		modCounter++;
	}

	@Override
	public Iterator<K> keys() {
		return new KeyIteratorHelper();
	}
	
	protected class KeyIteratorHelper implements Iterator<K> { 
		@SuppressWarnings("rawtypes")
		private DictionaryNode[] nodes;
		private int index;
		private long modificationCounter;
		@SuppressWarnings({ "rawtypes" })
		public KeyIteratorHelper() {
			nodes =  new DictionaryNode[currentSize];
			index = 0;
			int j = 0;
			modificationCounter = modCounter;
			
			for (int i = 0; i < tableSize; i++) {
				if (!hArray[i].isEmpty()) {    //we cannot insert a node to our list if in there are no nodes in that table index 
					LinkedListDS<DictionaryNode<K,V>> list = hArray[i]; //we make a list of all our nodes 
					for (DictionaryNode n: list) 
						nodes[j++] =  n;
				}
			}
				
			nodes = (DictionaryNode[]) sorter(nodes);
		}
		@Override
		public boolean hasNext() {
			if (modificationCounter != modCounter)
				throw new ConcurrentModificationException();
			return index < nodes.length;
		}
		@SuppressWarnings("unchecked")
		@Override
		public K next() {
			if (!hasNext())
				throw new NoSuchElementException();
			return (K) nodes[index++].key;
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	 
	@Override
	public Iterator<V> values() {
		return new ValueIteratorHelper();
	}
	protected class ValueIteratorHelper implements Iterator<V> {
		Iterator<K> iter;
		public ValueIteratorHelper() {
			iter = keys(); //follows the order of the keys
		}
		@Override
		public boolean hasNext() {
			return iter.hasNext();   
		}

		@Override
		public V next() {
			return get(iter.next());
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private DictionaryNode[] sorter(DictionaryNode[] nodes) { //sorting method
		
		int end = nodes.length - 1; //start at the end 
		DictionaryNode<K,V> temp = null; //temp is used for swapping the elements
		for (int i = end; i >= 0; i--) {
			for (int j = i - 1; j >= 0; j--) {
				if (((Comparable<K>) nodes[i].key).compareTo((K) nodes[j].key) < 0) { //if two elements are not in the correct order, swap
					temp = nodes[i];
					nodes[i] = nodes[j];
					nodes[j] = temp;
				}
			}
		}
		
        return nodes; //return the ordered nodes 
	}
	
	
}

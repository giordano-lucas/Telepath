package bptree.impl;

import PageCacheSort.SetIterator;
import bptree.PageProxyCursor;
import org.neo4j.io.pagecache.PagedFile;

import java.io.IOException;

/**
 * Created by max on 6/9/15.
 */
public class DiskCompressor {

    public static long finalPageID = 0;

    public static DiskCache convertDiskToCompressed(SetIterator iterator, int keyLength) throws IOException {
        long[] next;
        long[] prev = new long[keyLength];
        byte[] encodedKey;
        int keyCount = 0;
        DiskCache compressedDisk = DiskCache.persistentDiskCache("compressed_disk.dat", false); //I'm handling compression here, so I don't want the cursor to get confused.
        try(PageProxyCursor compressedCursor = compressedDisk.getCursor(0, PagedFile.PF_EXCLUSIVE_LOCK)) {
            compressedCursor.setOffset(NodeHeader.NODE_HEADER_LENGTH);
            while(iterator.hasNext()){
                next = iterator.getNext();
                encodedKey = encodeKey(next, prev);
                prev = next;
                keyCount++;
                compressedCursor.putBytes(encodedKey);
                if(compressedCursor.getOffset() + (keyLength * Long.BYTES) > DiskCache.PAGE_SIZE){
                    //finalize this buffer, write it to the page, and start a new buffer
                    NodeHeader.setFollowingID(compressedCursor, compressedCursor.getCurrentPageId() + 1);
                    NodeHeader.setPrecedingId(compressedCursor, compressedCursor.getCurrentPageId() - 1);
                    NodeHeader.setKeyLength(compressedCursor, keyLength);
                    NodeHeader.setNumberOfKeys(compressedCursor, keyCount);
                    NodeHeader.setNodeTypeLeaf(compressedCursor);
                    compressedCursor.next(compressedCursor.getCurrentPageId() + 1);
                    compressedCursor.setOffset(NodeHeader.NODE_HEADER_LENGTH);
                    keyCount = 0;
                    prev = new long[keyLength];
                }
            }
            NodeHeader.setFollowingID(compressedCursor, -1);//last leaf node
            NodeHeader.setPrecedingId(compressedCursor, compressedCursor.getCurrentPageId() - 1);
            NodeHeader.setKeyLength(compressedCursor, keyLength);
            NodeHeader.setNumberOfKeys(compressedCursor, keyCount);
            NodeHeader.setNodeTypeLeaf(compressedCursor);
            finalPageID = compressedCursor.getCurrentPageId();
        }
        compressedDisk.COMPRESSION = true;
        return compressedDisk;
    }

    public static byte[] encodeKey(long[] key, long[] prev){

        long[] diff = new long[key.length];
        for(int i = 0; i < key.length; i++)
        {
            diff[i] = key[i] - prev[i];
        }

        int maxNumBytes = Math.max(numberOfBytes(diff[0]), 1);
        for(int i = 1; i < key.length; i++){
            maxNumBytes = Math.max(maxNumBytes, numberOfBytes(diff[i]));
        }

        byte[] encoded = new byte[1 + (maxNumBytes * 3 )];
        encoded[0] = (byte)maxNumBytes;
        for(int i = 0; i < key.length; i++){
            toBytes(diff[i], encoded, 1 + (i * maxNumBytes), maxNumBytes);
        }
        return encoded;
    }

    public static int numberOfBytes(long value){
        return (int) (Math.ceil(Math.log(value) / Math.log(2)) / 8) + 1;
    }

    public static void toBytes(long val, byte[] dest,int position, int numberOfBytes) { //rewrite this to put bytes in a already made array at the right position.
        for (int i = numberOfBytes - 1; i > 0; i--) {
            dest[position + i] = (byte) val;
            val >>>= 8;
        }
        dest[position] = (byte) val;
    }
    public static byte[] toBytes(long val) {
        int numberOfBytes = (int) (Math.ceil(Math.log(val) / Math.log(2)) / 8) + 1;
        byte [] b = new byte[numberOfBytes];
        for (int i = numberOfBytes - 1; i > 0; i--) {
            b[i] = (byte) val;
            val >>>= 8;
        }
        b[0] = (byte) val;
        return b;
    }

    public static long toLong(byte[] bytes) {
        long l = 0;
        for(int i = 0; i < bytes.length; i++) {
            l <<= 8;
            l ^= bytes[i] & 0xFF;
        }
        return l;
    }

    public static long toLong(byte[] bytes, int offset, int length) {
        long l = 0;
        for(int i = offset; i < offset + length; i++) {
            l <<= 8;
            l ^= bytes[i] & 0xFF;
        }
        return l;
    }
}
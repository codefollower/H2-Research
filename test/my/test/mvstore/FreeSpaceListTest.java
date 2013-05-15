package my.test.mvstore;

import org.h2.mvstore.Chunk;
import org.h2.mvstore.FreeSpaceList;

public class FreeSpaceListTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FreeSpaceList fsl = new FreeSpaceList();

		int length = 4096 * 2;
		int pagePos = fsl.allocatePages(length);
		System.out.println(pagePos);

		//前两页固定给FileHeader了，相当于0号和1号page不能分配，只能从2号page开始
		int start = 4096 * 2;
		
		System.out.println(fsl);

		Chunk c1 = getChunk(start, length);
		fsl.markUsed(c1);
		
		fsl.markFree(c1);
		
		System.out.println(fsl);

		start = 4096 * 2 * 2 * 2;
		Chunk c2 = getChunk(start, length);
		fsl.markUsed(c2);
		
		System.out.println(fsl);
		fsl.markFree(c2);
		System.out.println(fsl);

		Chunk c3 = getChunk(4096 * 2 * 3, 4096 / 2);
		fsl.markUsed(c3);

		System.out.println(fsl);
		
		Chunk c4 = getChunk(4096 * 14, 4096 * 86);
		fsl.markUsed(c4);

		
		fsl.markFree(c1);
		
		System.out.println(fsl);
		
		fsl.markFree(c3);

		System.out.println(fsl);
		
		//fsl.markFree(c2);
		
		fsl.markFree(c4);

		System.out.println(fsl);

	}

	static Chunk getChunk(int start, int length) {
		String chunkString = "id:4,length:" + length
				+ ",maxLength:0,maxLengthLive:0,metaRoot:0,pageCount:0,pageCountLive:0,start:" + start + ",time:16,version:4";
		Chunk c = Chunk.fromString(chunkString);

		return c;
	}
}

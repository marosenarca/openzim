/*
 * This file is a part of zimreader-java.
 *
 * zimreader-java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as 
 * published by the Free Software Foundation.
 *
 * zimreader-java is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with zimreader-java.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.openzim.ZIMTypes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.tukaani.xz.SingleXZInputStream;
import org.openzim.util.RandomAcessFileZIMInputStream;
import org.openzim.util.Utilities;

/**
 * @author Arunesh Mathur
 * 
 *         A ZIMReader that reads data from the ZIMFile
 * 
 */
public class ZIMReader {

	private ZIMFile mFile;
	private RandomAcessFileZIMInputStream mReader;

	public ZIMReader(ZIMFile file) {
		this.mFile = file;
		try {
			mReader = new RandomAcessFileZIMInputStream(new RandomAccessFile(
					mFile, "r"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public List<String> getURLListByURL() throws IOException {

		int i = 0, pos, mimeType;

		byte[] buffer = new byte[8];

		// The list that will eventually return the list of URL's
		ArrayList<String> returnList = new ArrayList<String>();

		// Move to the spot where URL's are listed
		mReader.seek(mFile.getUrlPtrPos());

		for (i = 0; i < mFile.getArticleCount(); i++) {

			// The position of URL i
			pos = mReader.readEightLittleEndianBytesValue(buffer);

			// Mark the current position that we need to return to
			mReader.mark();

			// Move to the position of URL i
			mReader.seek(pos);

			mimeType = mReader.readTwoLittleEndianBytesValue(buffer); // Article
																		// or
																		// Redirect
																		// entry?

			// TODO: Improve conditions to match
			if (mimeType == 65535) {
				mReader.seek(pos + 12);

				String url = mReader.readString();
				returnList.add(url);

				// System.out.println("RE: " + url);
			} else {
				mReader.seek(pos + 16);

				// Read the URL
				String url = mReader.readString();

				// Add it to the list
				returnList.add(url);

				// System.out.println("AE: " + url);
			}

			mReader.reset();
		}

		return returnList;
	}

	public List<String> getURLListByTitle() throws IOException {

		int i = 0, pos, mimeType, articleNumber, urlPtrPos;

		byte[] buffer = new byte[8];

		// The list that will eventually return the list of URL's
		ArrayList<String> returnList = new ArrayList<String>();

		// Get the UrlPtrPos or one time storage
		urlPtrPos = mFile.getUrlPtrPos();

		// Move to the spot where URL's are listed
		mReader.seek(mFile.getTitlePtrPos());

		for (i = 0; i < mFile.getArticleCount(); i++) {

			// The articleNumber of the position of URL i
			articleNumber = mReader.readFourLittleEndianBytesValue(buffer);

			// Mark the current position that we need to return to
			mReader.mark();

			mReader.seek(urlPtrPos + (8 * (articleNumber)));

			// The position of URL i
			pos = mReader.readEightLittleEndianBytesValue(buffer);
			mReader.seek(pos);

			mimeType = mReader.readTwoLittleEndianBytesValue(buffer); // Article
																		// or
																		// Redirect
																		// entry?

			// TODO: Improve conditions to match
			if (mimeType == 65535) {
				mReader.seek(pos + 12);

				String url = mReader.readString();
				returnList.add(url);

				// System.out.println("RE: " + url);
			} else {
				mReader.seek(pos + 16);

				String url = mReader.readString();
				returnList.add(url);

				// System.out.println("AE: " + url);
			}

			// Return to the marked position
			mReader.reset();
		}

		return returnList;
	}

	// Gives the minimum required information needed for the given articleName
	// TODO: Extend this function so that it gives info like the Zimlib
	public DirectoryEntry getDirectoryInfo(String articleName)
			throws IOException {
		List<String> listParam = getURLListByURL();
		byte[] buffer = new byte[8];

		// TODO: Change indexOf to a binary search
		int urlListindex = listParam.indexOf(articleName), pos;

		if (urlListindex != -1) {

			// Move to the article at index
			mReader.seek(mFile.getUrlPtrPos() + urlListindex * 8);

			// Get value of article at index
			pos = mReader.readEightLittleEndianBytesValue(buffer);

			// Go to the location of the directory entry
			mReader.seek(pos);

			int type = mReader.readTwoLittleEndianBytesValue(buffer);

			// Ignore the parameter length
			mReader.read();

			char namespace = (char) mReader.read();
			// System.out.println("Namepsace: " + namespace);

			int revision = mReader.readFourLittleEndianBytesValue(buffer);
			// System.out.println("Revision: " + revision);

			// TODO: Remove redundant if condition code
			// Article or Redirect entry
			if (type == 65535) {

				// System.out.println("MIMEType: " + type);

				int redirectIndex = mReader
						.readFourLittleEndianBytesValue(buffer);
				// System.out.println("RedirectIndex: " + redirectIndex);

				String url = mReader.readString();
				// System.out.println("URL: " + url);

				String title = mReader.readString();
				title = title.equals("") ? url : title;
				// System.out.println("Title: " + title);

				return new RedirectEntry(type, namespace, revision,
						redirectIndex, url, title, urlListindex);

			} else {

				// System.out.println("MIMEType: " + mFile.getMIMEType(type));

				int clusterNumber = mReader
						.readFourLittleEndianBytesValue(buffer);
				// System.out.println("Cluster Number: " + clusterNumber);

				int blobNumber = mReader.readFourLittleEndianBytesValue(buffer);
				// System.out.println("Blob Number: " + blobNumber);

				String url = mReader.readString();
				// System.out.println("URL: " + url);

				String title = mReader.readString();
				title = title.equals("") ? url : title;
				// System.out.println("Title: " + title);

				// Parameter data ignored

				return new ArticleEntry(type, namespace, revision,
						clusterNumber, blobNumber, url, title, urlListindex);
			}
		}
		return null;
	}

	// TODO: IMPORTANT. Make it cleaner by using SKIP()
	public String getArticleData(String articleName)
			throws IOException {

		byte[] buffer = new byte[8];

		DirectoryEntry mainEntry = getDirectoryInfo(articleName);

		// Check what kind of an entry was mainEnrty
		if (mainEntry.getClass() == ArticleEntry.class) {

			// Cast to ArticleEntry
			ArticleEntry article = (ArticleEntry) mainEntry;

			// Get the cluster and blob numbers from the article
			int clusterNumber = article.getClusterNumber();
			int blobNumber = article.getBlobnumber();

			// Move to the cluster entry in the clusterPtrPos
			mReader.seek(mFile.getClusterPtrPos() + clusterNumber * 8);

			// Read the location of the cluster
			int clusterPos = mReader.readEightLittleEndianBytesValue(buffer);

			// Move to the cluster
			mReader.seek(clusterPos);

			// Read the first byte, for compression information
			int compressionType = mReader.read();

			// Reference declaration
			SingleXZInputStream xzReader = null;

			// Check the compression type that was read
			switch (compressionType) {

			// TODO: Read uncompressed data directly
			case 0:
			case 1:
				break;

			// LZMA2 compressed data
			case 4:

				// Read the first 4 bytes to find out the number of artciles
				buffer = new byte[4];

				// Create a dictionary with size 40KiB
				xzReader = new SingleXZInputStream(mReader, 4194304);

				// Read the first offset
				xzReader.read(buffer);

				// The first four bytes are the offset of the zeroth blob
				int firstOffset = Utilities.toFourLittleEndianInteger(buffer);

				// The number of blobs
				int numberOfBlobs = firstOffset / 4;

				// The blobNumber has to be lesser than the numberOfBlobs
				assert blobNumber < numberOfBlobs;
			
				int offset1,offset2,location,differenceOffset;

				if(blobNumber==0) {
					offset1 = firstOffset;					
				} else {
					location = (blobNumber-1) * 4;
					Utilities.skipFully(xzReader,location);
					xzReader.read(buffer);
					offset1 = Utilities.toFourLittleEndianInteger(buffer);					
				}

				xzReader.read(buffer);
				offset2 = Utilities.toFourLittleEndianInteger(buffer);					
				
				differenceOffset = offset2-offset1;
				buffer = new byte[differenceOffset];

				Utilities.skipFully(xzReader,(offset1 - 4*(blobNumber+2)));

				xzReader.read(buffer,0,differenceOffset);
			
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				baos.write(buffer,0,differenceOffset);

				return baos.toString("utf-8");
				
			}
		}

		return null;
	}
}

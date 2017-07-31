package bdv.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;

import bdv.bigcat.label.FragmentSegmentAssignment;
import bdv.bigcat.ui.GoldenAngleSaturatedARGBStream;
import bdv.bigcat.ui.VolatileLabelMulisetNonVolatileARGBConverter;
import bdv.cache.CacheControl;
import bdv.labels.labelset.ByteUtils;
import bdv.labels.labelset.Label;
import bdv.labels.labelset.LongMappedAccessData;
import bdv.labels.labelset.N5CacheLoader;
import bdv.labels.labelset.VolatileLabelMultisetArray;
import bdv.labels.labelset.VolatileLabelMultisetType;
import bdv.util.LocalIdService;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerFrame;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.ref.BoundedSoftRefLoaderCache;
import net.imglib2.cache.util.LoaderCacheAsCacheAdapter;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class BigDataViewerN5Demo {

	public static void main(String[] args) throws IOException {
		CachedCellImg<VolatileLabelMultisetType, VolatileLabelMultisetArray> fullRes = loadFromN5("/home/thistlethwaiten/CREMI-N5", "sampleA-fullres-gz");
		CachedCellImg<VolatileLabelMultisetType, VolatileLabelMultisetArray> down1 = loadFromN5("/home/thistlethwaiten/CREMI-N5", "sampleA-4x4x1-gz");
		CachedCellImg<VolatileLabelMultisetType, VolatileLabelMultisetArray> down2 = loadFromN5("/home/thistlethwaiten/CREMI-N5", "sampleA-16x16x4-gz");
		CachedCellImg<VolatileLabelMultisetType, VolatileLabelMultisetArray> down3 = loadFromN5("/home/thistlethwaiten/CREMI-N5", "sampleA-32x32x8-gz");
		
		LongMappedAccessData store = LongMappedAccessData.factory.createStorage(32);
		ByteUtils.putLong(Label.OUTSIDE, store.getData(), 0 );
		ByteUtils.putInt(1, store.getData(), 8 );
		VolatileLabelMultisetType extension = new VolatileLabelMultisetType(new VolatileLabelMultisetArray(new int[] { 0 }, store, true), true);
		
		System.out.println(extension.get());
		
		long[] actualDimensions = new long[] {
			( down3.dimension(0) + 1 ) * 32,
			( down3.dimension(1) + 1 ) * 32,
			( down3.dimension(2) + 1 ) * 8
		};
		System.out.println(Arrays.toString(actualDimensions ) );

		RandomAccessibleInterval<VolatileLabelMultisetType>[] rais = new RandomAccessibleInterval[] {
				fullRes, 
				Views.interval(down1, new FinalInterval( new long[] { 312, 312, 124 } ) ),
				Views.interval(down2, new FinalInterval( new long[] { 78, 78, 31 } ) ),
				Views.interval(down3, new FinalInterval( new long[] { 39, 39, 15 } ) )
				};

		System.out.println(Arrays.toString(Intervals.dimensionsAsLongArray(fullRes)));
		System.out.println(Arrays.toString(Intervals.dimensionsAsLongArray(down1)));
		System.out.println(Arrays.toString(Intervals.dimensionsAsLongArray(down2)));
		System.out.println(Arrays.toString(Intervals.dimensionsAsLongArray(down3)));
		RandomAccessibleIntervalMipmapSource<VolatileLabelMultisetType> mipmapsource = new RandomAccessibleIntervalMipmapSource<>(rais, new VolatileLabelMultisetType(),
				new double[][] { {1,1,4}, {4,4,4}, {16,16,16}, {32,32,32} }, null, "rai multiscale");
//		RandomAccessibleIntervalMipmapSource<VolatileLabelMultisetType> mipmapsource = new RandomAccessibleIntervalMipmapSource<>(rais, new VolatileLabelMultisetType(),
//				new double[][] { {1,1,4} }, null, "rai singlescale");
//				new double[][] { {1,1,1}, {2,2,2}, {4,4,4} }, null, "rai multiscale");
		
		ViewerFrame vf = new ViewerFrame(new ArrayList<>(), 1, new CacheControl.Dummy());
		
		final GoldenAngleSaturatedARGBStream argbStream = new GoldenAngleSaturatedARGBStream( new FragmentSegmentAssignment( new LocalIdService() ) );
		
		vf.getViewerPanel().addSource(new SourceAndConverter<VolatileLabelMultisetType>(mipmapsource, new VolatileLabelMulisetNonVolatileARGBConverter(argbStream)  ));
		System.out.println("done");
		long bg = Label.OUTSIDE;
		
		vf.setVisible(true);
	}
	
	public static CachedCellImg<VolatileLabelMultisetType, VolatileLabelMultisetArray> loadFromN5(String groupName, String datasetName) throws IOException {
		
		N5Reader reader = new N5FSReader(groupName);
		DatasetAttributes attr = reader.getDatasetAttributes(datasetName);
		
		long[] dimensions = attr.getDimensions();
		int[] blocksize = attr.getBlockSize();
		
		int nDim = dimensions.length;
		final long[] offset = new long[nDim];
		
		final CacheLoader< Long, Cell< VolatileLabelMultisetArray > > cacheLoader = new N5CacheLoader(reader, datasetName);
		
		final BoundedSoftRefLoaderCache< Long, Cell< VolatileLabelMultisetArray > > cache = new BoundedSoftRefLoaderCache<>( 1 );
		final LoaderCacheAsCacheAdapter< Long, Cell< VolatileLabelMultisetArray > > wrappedCache = new LoaderCacheAsCacheAdapter<>( cache, cacheLoader );
		
		return new CachedCellImg<VolatileLabelMultisetType,VolatileLabelMultisetArray>(	new CellGrid(dimensions, blocksize),
				new VolatileLabelMultisetType(), wrappedCache, new VolatileLabelMultisetArray(0, true));
	}
}

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.slicer.SlicerViews;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.sourceandconverter.transform.SourceMosaicZSlicer;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.XmlIoSpimData;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;


/**
 * NOT WORKING!! A Clone for affine transform or for the outofbounds stuff should be put somewhere
 *
 */
public class DemoZSlicedSource {

    static {
        LegacyInjector.preinit();
    }

    static final ImageJ ij = new ImageJ();

    static public void main(String... args) throws Exception {

        ij.ui().showUI();

        // load and convert the famous blobs image// Gets active BdvHandle instance
        BdvHandle bdv = SourceAndConverterServices.getBdvDisplayService().getActiveBdv();
        // Import SpimData
        new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").run();

        final List<SourceAndConverter<?>> sacs = SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters();

        sacs.forEach( sac -> {
            SourceAndConverterServices.getBdvDisplayService().show( bdv, sac );
            new ViewerTransformAdjuster( bdv, sac ).run();
            new BrightnessAutoAdjuster( sac, 0 ).run();
        } );

        RandomAccessibleInterval nonResliced = sacs.get(0).getSpimSource().getSource(0,0);

        ExtendedRandomAccessibleInterval rai = SlicerViews.extendSlicer(nonResliced,2,0);

        BdvFunctions.show(
        Views.interval(rai,
                new FinalInterval(nonResliced.dimension(0)*nonResliced.dimension(2),
                        nonResliced.dimension(1),
                        1)
                       ),
                "Sliced"
                );

    }

}

package ch.epfl.biop.scijava.command.spimdata;

import ch.epfl.biop.bdv.img.OpenersToSpimData;
import ch.epfl.biop.bdv.img.bioformats.BioFormatsHelper;
import ch.epfl.biop.bdv.img.legacy.bioformats.entity.FileIndex;
import ch.epfl.biop.bdv.img.opener.OpenerSettings;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import spimdata.SpimDataHelper;
import spimdata.util.Displaysettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDVDataset>Edit>Make CZI Dataset for BigStitcher")
public class CreateCZIDatasetCommand implements Command {

    @Parameter(style = "open")
    File czi_file;

    @Parameter
    Context ctx;

    @Parameter(type = ItemIO.OUTPUT)
    File xml_out;

    @Parameter(label = "Output directory", style = "directory", required = false, persist = false)
    File output_folder = null;

    @Override
    public void run() {

        if (output_folder == null) {
            xml_out = new File(czi_file.getParent(), FilenameUtils.removeExtension(czi_file.getName()) + ".xml");
        } else {
            xml_out = new File(output_folder, FilenameUtils.removeExtension(czi_file.getName()) + ".xml");
        }
        if (xml_out.exists()) {
            IJ.error("The output file already exist! Skipping execution");
            return;
        }
        // We need to:
        // make a spimdata
        // rescale it
        // get rid of extra attributes
        // save the xml at the same place as the file
        String bfOptions = "--bfOptions zeissczi.autostitch=false";
        List<OpenerSettings> openerSettings = new ArrayList<>();
        int nSeries = BioFormatsHelper.getNSeries(czi_file, bfOptions);
        for (int i = 0; i < nSeries; i++) {
            openerSettings.add(
                    OpenerSettings.BioFormats()
                            .location(czi_file)
                            .setSerie(i)
                            .micrometer()
                            .splitRGBChannels(true)
                            .cornerPositionConvention()
                            .addOptions(bfOptions)
                            .context(ctx));
        }
        AbstractSpimData<?> asd = OpenersToSpimData.getSpimData(openerSettings);

        // Remove display settings attributes because this causes issues with BigStitcher
        SpimDataHelper.removeEntities(asd, Displaysettings.class, FileIndex.class);

        double pixSizeXYMicrometer = asd.getViewRegistrations().getViewRegistration(0,0).getModel().get(0,0);

        double scalingForBigStitcher = 1 / pixSizeXYMicrometer;

        // Scaling such as size of one pixel = 1
        SpimDataHelper.scale(asd, "BigStitcher Scaling", scalingForBigStitcher);

        asd.setBasePath(new File(xml_out.getAbsolutePath()).getParentFile());
        try {
            new XmlIoSpimData().save((SpimData) asd, xml_out.getAbsolutePath());
        } catch (SpimDataException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }


    }
}

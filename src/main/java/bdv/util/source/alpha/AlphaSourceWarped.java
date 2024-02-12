package bdv.util.source.alpha;

import bdv.img.WarpedSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import net.imglib2.*;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Alpha Source of a {@link WarpedSource}
 *
 * The transformation is synchronized because this source knows which source it is the alpha of.
 *
 */

public class AlphaSourceWarped extends AlphaSource {

    final WarpedSource origin_warped;

    IAlphaSource origin_alpha;

    public AlphaSourceWarped(Source<?> origin) {
        super(origin);
        assert origin instanceof WarpedSource;
        origin_warped = (WarpedSource) origin;
    }

    public AlphaSourceWarped(Source<?> origin, float alpha) {
        super(origin, alpha);
        origin_warped = (WarpedSource) origin;
    }

    public IAlphaSource getAlpha() {
        if (origin_alpha==null) {
            origin_alpha = (IAlphaSource) AlphaSourceHelper.getOrBuildAlphaSource(((WarpedSource<?>) origin).getWrappedSource()).getSpimSource();
        }
        return origin_alpha;
    }

	@Override
	public RandomAccessibleInterval<FloatType> getSource(int t, int level) {
		final RealRandomAccessible<FloatType> rra = getInterpolatedSource(t, level, Interpolation.NEARESTNEIGHBOR);
		return Views.interval(Views.raster(rra), origin_warped.getSource(t, level));
	}

    @Override
    public RealRandomAccessible<FloatType> getInterpolatedSource(int t, int level, Interpolation method) {
		RealRandomAccessible<FloatType> sourceRealAccessible = getAlpha().getInterpolatedSource(t, level, method);
		if (origin_warped.isTransformed()) {

			final AffineTransform3D transform = new AffineTransform3D();
			getAlpha().getSourceTransform(t, level, transform);
			final RealRandomAccessible<FloatType> srcRa = getAlpha().getInterpolatedSource(t, level, method);

			if (origin_warped.getTransform() == null)
				return srcRa;
			else {
				final RealTransformSequence seq = new RealTransformSequence();
				// build the inverse transform
				seq.add(transform);
				seq.add(origin_warped.getTransform().copy());
				seq.add(transform.inverse());
				return new RealTransformRealRandomAccessible(srcRa, seq);
			}

		} else {
			return sourceRealAccessible;
		}
    }

    @Override
    public boolean intersectBox(AffineTransform3D affineTransform, Interval cell, int timepoint) {
        // How to do better ? We know nothing about the warping
        return true;
    }
}

/* Copyright (C) 2019 Portland State University
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public License
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For any questions regarding the license, please contact the Free Software
 * Foundation.  For any other questions regarding this program, please contact
 * David Cohoe at dcohoe@pdx.edu.
 */

package edu.pdx.imagej.phase_unwrapping;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import net.imagej.ops.OpService;

@Plugin(type = Command.class,
        menuPath = "Plugins>DHM>Phase Unwrapping>Double Wavelength")
public class DoubleWavelengthCommand implements Command {
    @Parameter(label = "Phase Image 1")
    private ImagePlus M_phase_image1;
    @Parameter(label = "Wavelength 1 (nm)")
    private int M_wavelength1;
    @Parameter(label = "Phase Image 2")
    private ImagePlus M_phase_image2;
    @Parameter(label = "Wavelength 2 (nm)")
    private int M_wavelength2;
    @Parameter(label = "Pixel phase value")
    // The main reason this is not τ is because having it τ when it should be
    // 256 makes it look good when it shouldn't, but having it 256 when it
    // should be τ makes it look bad when it should.
    private float M_phase = 256.0f;
    @Parameter(label = "Debug")
    private boolean M_debug = false;
    @Parameter OpService P_ops;

    private float M_combined_wavelength;

    @Override
    public void run() {
        int width = M_phase_image1.getWidth();
        int height = M_phase_image1.getHeight();
        ImageStack coarse_stack = new ImageStack(width, height);
        ImageStack fine_stack = new ImageStack(width, height);
        ImageStack[] debug_stack = null;
        if (M_debug) {
            debug_stack = new ImageStack[7];
            for (int i = 0; i < 7; ++i) {
                debug_stack[i] = new ImageStack(width, height);
            }
        }

        int t_size = Math.min(M_phase_image1.getNFrames(),
                              M_phase_image2.getNFrames());
        int z_size = Math.min(M_phase_image1.getNSlices(),
                              M_phase_image2.getNSlices());
        int final_size = t_size * z_size;
        int i = 0;
        for (int t = 1; t <= t_size; ++t) {
            for (int z = 1; z <= z_size; ++z) {
                if (final_size > 1) {
                    IJ.showProgress(i + 1, final_size);
                    ++i;
                }
                int current_slice1 = M_phase_image1.getStackIndex(1, z, t);
                int current_slice2 = M_phase_image2.getStackIndex(1, z, t);
                float[][] img1 = M_phase_image1.getStack()
                                                .getProcessor(current_slice1)
                                                .getFloatArray();
                float[][] img2 = M_phase_image2.getStack()
                                               .getProcessor(current_slice2)
                                               .getFloatArray();
                DoubleWavelengthOp.PhaseImage image1 =
                                            new DoubleWavelengthOp.PhaseImage();
                image1.phase_image = img1;
                image1.wavelength = M_wavelength1;
                image1.phase_value = M_phase;
                DoubleWavelengthOp.PhaseImage image2 =
                                            new DoubleWavelengthOp.PhaseImage();
                image2.phase_image = img2;
                image2.wavelength = M_wavelength2;
                image2.phase_value = M_phase;
                float[][][] result = (float[][][])P_ops.run(
                    "Double Wavelength Phase Unwrapping",
                    image1, image2, M_debug);
                if (M_debug) {
                    coarse_stack.addSlice(new FloatProcessor(result[3]));
                    fine_stack.addSlice(new FloatProcessor(result[6]));
                    for (int j = 0; j <= 6; ++j) {
                        debug_stack[j].addSlice(new FloatProcessor(result[j]));
                    }
                }
                else {
                    coarse_stack.addSlice(new FloatProcessor(result[0]));
                    fine_stack.addSlice(new FloatProcessor(result[1]));
                }
            }
        }
        if (M_debug) {
            show_stack(debug_stack[0], "Phase Image 1 (a)", z_size, t_size);
            show_stack(debug_stack[1], "Phase Image 2 (b)", z_size, t_size);
            show_stack(debug_stack[2], "Phase Difference (c)", z_size, t_size);
            show_stack(debug_stack[3], "Coarse Map (d)", z_size, t_size);
            show_stack(debug_stack[4], "Round to Phase 1 (e)", z_size, t_size);
            show_stack(debug_stack[5], "Round + Phase 1 (f)", z_size, t_size);
            show_stack(debug_stack[6], "Fine Map (g)", z_size, t_size);
        }
        else {
            show_stack(coarse_stack, "Coarse Map", z_size, t_size);
            show_stack(fine_stack, "Fine Map", z_size, t_size);
        }
    }
    private void show_stack(ImageStack stack, String label,
                            int z_size, int t_size)
    {
        ImagePlus imp = IJ.createHyperStack(
            label, stack.getWidth(), stack.getHeight(), 1, z_size, t_size, 32);
        imp.setStack(stack);
        imp.show();
    }
}
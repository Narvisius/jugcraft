/* SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
 * SPDX-License-Identifier: MPL-2.0
 * Adapted for Jugcraft from the owner-supplied cc/audio/dfpwm.lua predictor/decoder.
 * The original is retained under art/owner-library/originals/Oct23/Jimtafacts/computers/.
 */
package io.github.jimbozoomer.jugcraft.client.jimtafacts;

/** DFPWM1a decoder, 48 kHz signed samples; finite clips start with an empty predictor. */
public final class WorkshopDfpwm {
    private WorkshopDfpwm(){}
    public static byte[] decode(byte[] input){
        if(input.length>4096)throw new IllegalArgumentException("Clip limit");
        byte[] output=new byte[input.length*8];int index=0,charge=0,strength=0,lowPass=0,previousCharge=0;
        boolean previousBit=false;
        for(byte packed:input)for(int bit=0;bit<8;bit++){
            boolean current=((packed>>>bit)&1)!=0;int target=current?127:-128;
            int next=charge+Math.floorDiv(strength*(target-charge)+512,1024);
            if(next==charge&&next!=target)next+=current?1:-1;
            int wanted=current==previousBit?1023:0;if(strength!=wanted)strength+=current==previousBit?1:-1;strength=Math.max(8,strength);
            charge=next;int antiJerk=current==previousBit?charge:Math.floorDiv(charge+previousCharge+1,2);
            previousCharge=charge;previousBit=current;lowPass+=Math.floorDiv((antiJerk-lowPass)*140+128,256);
            output[index++]=(byte)Math.clamp(lowPass,-128,127);
        }return output;
    }
}

import ghidra.program.util.VarnodeContext;
import ghidra.program.model.symbol.Reference;
import java.util.ArrayList;

/**
 * Wrapper class for a single ISA instruction
 * (https://ghidra.re/ghidra_docs/api/ghidra/program/model/listing/Instruction.html).
 *
 * This model contains the list of pcode instructions representing a single ISA
 * instruction.
 * This class is used for clean and simple serialization.
 */
public class Instruction {
	private String mnemonic;
	private String address;
	private int size;
	private ArrayList<Term> terms = new ArrayList();
	private ArrayList<String> potential_targets;
	private String fall_through = null;


	public Instruction(ghidra.program.model.listing.Instruction instruction, VarnodeContext context, DatatypeProperties datatypeProperties) {
		this.mnemonic = instruction.toString();
		this.address = "0x" + instruction.getAddressString(false, false);
		this.size = instruction.getLength();

		if (instruction.getFallThrough() != null) {
			this.fall_through = "0x" + instruction.getFallThrough().toString(false, false);
		}


		ghidra.program.model.pcode.PcodeOp[] pcodes = instruction.getPcode(true);
		for (int i = 0; i < pcodes.length; i++) {
			this.terms.add(new Term(this.address, i, pcodes[i], context, datatypeProperties));

			// add potential targets if instruction contains indirect call or branch.
			// Note: All references are put together. Multiple CALLIND or BRANCHIND should not
			// occur within a single instruction, but if so, the potential targets are not
			// separable.
			if ((pcodes[i].getMnemonic() == "CALLIND") || (pcodes[i].getMnemonic() == "BRANCHIND")) {
				if (potential_targets == null) {
					potential_targets = new ArrayList<String>();
				}
				for (Reference ref : instruction.getReferencesFrom()) {
					switch (ref.getReferenceType().toString()) {
					case "COMPUTED_JUMP":
					case "CONDITIONAL_COMPUTED_JUMP":
						if (pcodes[i].getMnemonic() == "BRANCHIND") {
							potential_targets.add("0x" + ref.getToAddress().toString(false, false));
						}
						break;
					case "COMPUTED_CALL":
					case "CONDITIONAL_COMPUTED_CALL":
						if (pcodes[i].getMnemonic() == "CALLIND") {
							potential_targets.add("0x" + ref.getToAddress().toString(false, false));
						}
						break;
					default:
						break;
					}
				}
			}
		}
	}
}

// Skeleton file with hints about transient registers.

package sparc;

public class Codegen {

    SparcFrame frame;

    public Codegen(SparcFrame f) {frame = f;}

    // ilist holds the list of instructions generated so far.
    private assem.InstrList ilist = null, last = null;

    private void emit(assem.Instr inst) {
	// Add inst to the end of ilist.
	if (last != null)
	    last = last.tail = new assem.InstrList(inst, null);
	else
	    last = ilist = new assem.InstrList(inst, null);
    }

    // Two handy abbreviations:
    private void eOPER(String format, temp.TempList dst, temp.TempList src,
		       temp.LabelList jmp) {
	  emit(new assem.OPER(format, dst, src, jmp));
    }

    private void eOPER(String format, temp.TempList dst, temp.TempList src) {
	  emit(new assem.OPER(format, dst, src));
    }

    // It's handy to have an abbreviation for creating TempLists. (See p. 194.)
    static temp.TempList L(temp.Temp h, temp.TempList t) {
	  return new temp.TempList(h, t);
    }

    // Since Sparc instructions are often limited to 13-bit signed constants,
    // it's useful to be able to check easily for this case.
    static boolean is13bitCONST(tree.Exp e) {
	  if (e instanceof tree.CONST) {
	    int val = ((tree.CONST) e).value;
	    return (-4096 <= val && val < 4096);
	  }
	  else
	    return false;
    }

    // Here we reserve three fixed "transient" registers to reuse:
    private temp.Temp transient1 = frame.g1;
    static private temp.Temp transient2 = new temp.Temp();
    static private temp.Temp transient3 = new temp.Temp();

    void munchStm(tree.Stm s) {
	  if (s instanceof tree.MOVE) munchStm((tree.MOVE) s);
	  else if (s instanceof tree.EXPR) munchStm((tree.EXPR) s);
	  else if (s instanceof tree.JUMP) munchStm((tree.JUMP) s);
	  else if (s instanceof tree.CJUMP) munchStm((tree.CJUMP) s);
	  else if (s instanceof tree.LABEL) munchStm((tree.LABEL) s);
	  // Since we've canonicalized, tree.SEQ should not be a possibility.
	  else throw new Error("munchStm dispatch");
    }

    void munchStm(tree.EXPR s) {
	  munchExp(s.exp);
    }

    void munchStm(tree.JUMP s) {
	  eOPER("\tba\t`j0\n", null, null, s.targets);
	  //eOPER("\tnop\n", null, null);
    }

    void munchStm(tree.CJUMP s) {
	  if(s.left instanceof tree.CONST) {
	    tree.CONST left = ((tree.CONST)s.left);
	    if(is13bitCONST(left))
		  eOPER("\tcmp\t`s0, " + left.value + "\n", null, L(munchExp(s.right, transient1), null));
	    else {
		  eOPER("\tsethi\t%hi(" + left.value  +"), `d0\n", L(transient1, null), null);
		  eOPER("\tor\t`s0, %lo(" + left.value + "), `d0\n", L(transient1, null), L(transient1, null));
		  eOPER("\tcmp\t`s1, `s0\n", null, L(transient1, L(munchExp(s.right, transient2), null)));
	    }

	    switch(s.relop) {
		  case tree.CJUMP.EQ: eOPER("\tbe\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
		  case tree.CJUMP.NE:eOPER("\tbne\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
		  case tree.CJUMP.LT:eOPER("\tbg\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
		  case tree.CJUMP.GT:eOPER("\tbl\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
		  case tree.CJUMP.LE:eOPER("\tbge\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
		  case tree.CJUMP.GE: eOPER("\tble`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
		  default:throw new Error("Bad RELOP.");
	    }

	    eOPER("\tnop\n", null, null);

	    return;
	  }
	  else if(s.right instanceof tree.CONST) {
	    tree.CONST right = ((tree.CONST)s.right);
	    if(is13bitCONST(right))
		  eOPER("\tcmp\t`s0, " + ((tree.CONST)s.right).value + "\n", null, L(munchExp(s.left, transient1), null));
	    else {
		  eOPER("\tsethi\t%hi(" + right.value  +"), `d0\n", L(transient1, null), null);
		  eOPER("\tor\t`s0, %lo(" + right.value + "), `d0\n", L(transient1, null), L(transient1, null));
		  eOPER("\tcmp\t`s0, `s1\n", null, L(munchExp(s.left, transient2),L(transient1, null)));
	    }
	  }
	  else
	    eOPER("\tcmp\t`s0, `s1\n", null, L(munchExp(s.left), L(munchExp(s.right),null)));

	  switch(s.relop) {
	    case tree.CJUMP.EQ: eOPER("\tbe\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
	    case tree.CJUMP.NE:eOPER("\tbne\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
	    case tree.CJUMP.LT:eOPER("\tbl\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
	    case tree.CJUMP.GT:eOPER("\tbg\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
	    case tree.CJUMP.LE:eOPER("\tble\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
	    case tree.CJUMP.GE: eOPER("\tbge\t`j0\n", null, null, new temp.LabelList(s.iftrue, null)); break;
	    default:throw new Error("Bad RELOP.");
	  }
	  eOPER("\tnop\n", null, null);
    }

    void munchStm(tree.LABEL s) {
	  eOPER(((tree.LABEL)s).label.toString() + ":\n", null, null);
    }

    void munchStm(tree.MOVE s) {
	  if (s.dst instanceof tree.TEMP) {
	    tree.TEMP dest = ((tree.TEMP)s.dst);
	    if(s.src instanceof tree.CONST) {
		  tree.CONST src = ((tree.CONST)s.src);
		  if(is13bitCONST(s.src)) {
		    eOPER("\tmov\t" + src.value + ", `d0\n", L(dest.temp, null), null);
		  }
		  else {
		    eOPER("\tsethi\t%hi(" + src.value  +"), `d0\n", L(dest.temp, null), null);
		    eOPER("\tor\t`s0, %lo(" + src.value + "), `d0\n", L(dest.temp, null), L(dest.temp, null));
		  }
	    }
	    else if(s.src instanceof tree.MEM) {
		  tree.MEM location = (tree.MEM)s.src;
		  if(location.exp instanceof tree.BINOP) {
		    tree.BINOP binexp = (tree.BINOP)location.exp;
		    if(binexp.binop == tree.BINOP.PLUS) {
			  if(binexp.left instanceof tree.CONST) {
			    tree.CONST left = (tree.CONST)binexp.left;
			    eOPER("\tld\t[`s0 + " + left.value  + "], `d0\n", L(dest.temp, null), L(munchExp(binexp.right, transient3), null));
			  }
			  else if(binexp.right instanceof tree.CONST) {
			    tree.CONST right = (tree.CONST)binexp.right;
			    eOPER("\tld\t[`s0 + " + right.value  + "], `d0\n", L(dest.temp, null), L(munchExp(binexp.left, transient3), null));
			  }
			  else
			    eOPER("\tld\t[`s0 + `s1], `d0\n", L(dest.temp, null), L(munchExp(binexp.left, transient3),
										   L(munchExp(binexp.right, transient2),
										     null)));
		    }
		  }
		  else
		    eOPER("\tld\t[`s0 + `s1], `d0\n", L(dest.temp, null),  L(munchExp(s.src), L(frame.g0, null)));
	    }
	    else {
		  eOPER("\tmov\t`s0, `d0\n", L(dest.temp, null), L(munchExp(s.src, null), null));
	    }
	  }
	  else if (s.dst instanceof tree.MEM) {
	    tree.MEM location = (tree.MEM)s.dst;
	    if(location.exp instanceof tree.BINOP) {
		  tree.BINOP binexp = (tree.BINOP)location.exp;
		  if(binexp.binop == tree.BINOP.PLUS) {
		    if(binexp.left instanceof tree.CONST) {
			  tree.CONST left = (tree.CONST)binexp.left;
			  eOPER("\tst\t`s1, [`s0 + " + left.value  + "]\n", null, L(munchExp(binexp.right, transient3),L(munchExp(s.src, transient2), null)));
		    }
		    else if(binexp.right instanceof tree.CONST) {
			  tree.CONST right = (tree.CONST)binexp.right;
			  eOPER("\tst\t`s1, [`s0 + " + right.value  + "]\n", null, L(munchExp(binexp.left, transient3),L(munchExp(s.src, transient2), null)));
		    }
		    else
			  eOPER("\tst\t`s2, [`s0 + `s1]\n", null, L(munchExp(binexp.left),
								  L(munchExp(binexp.right),
								    L(munchExp(s.src),null))));
		  }
		  else
		    throw new Error("Bad MOVE");

	    }
	    else
		  eOPER("\tst\t`s1, [`s0 + `s2]\n", null, L(munchExp(location, transient3), L(munchExp(s.src), L(frame.g0, null))));
	  }
	  else {
	    System.out.println(s.dst.toString());
	    throw new Error("Bad MOVE destination.");
	  }
    }

    // // Here is munchExp as specified by Appel on p. 193.
    temp.Temp munchExp(tree.Exp e) {
      return munchExp(e, null);
    }

    // I give munchExp an extra parameter r that can specify a Temp in which
    // the result can safely be put. If r is null, then munchExp must come up
    // with a suitable Temp on its own (usually by generating a fresh one).
    temp.Temp munchExp(tree.Exp e, temp.Temp r) {
	  if (e instanceof tree.CONST) return munchExp((tree.CONST) e, r);
	  if (e instanceof tree.NAME) return munchExp((tree.NAME) e, r);
	  if (e instanceof tree.TEMP) return munchExp((tree.TEMP) e, r);
	  if (e instanceof tree.BINOP) return munchExp((tree.BINOP) e, r);
	  if (e instanceof tree.MEM) return munchExp((tree.MEM) e, r);
	  if (e instanceof tree.CALL) return munchExp((tree.CALL) e, r);
	  // Since we've canonicalized, tree.ESEQ should not be a possibility.
	  else throw new Error("munchExp dispatch");
    }

    assem.InstrList codegen(tree.Stm s) {
	  assem.InstrList l;

	  munchStm(s);
	  l = ilist;
	  ilist = last = null;
	  return l;
    }

    temp.Temp munchExp(tree.CONST c, temp.Temp r) {
	  temp.Temp reg;
	  reg = (r == null) ? new temp.Temp() : r;
	  if(is13bitCONST(c))
	    eOPER("\tmov\t" +c.value + ", `d0\n", L(reg, null), null);
	  else {
	    eOPER("\tsethi\t%hi(" + c.value  +"), `d0\n", L(reg, null), null);
	    eOPER("\tor\t`s0, %lo(" + c.value + "), `d0\n", L(reg, null), L(reg, null));
	  }

	  return reg;
    }

    temp.Temp munchExp(tree.NAME n, temp.Temp r) {
	  return null;
    }

    temp.Temp munchExp(tree.TEMP n, temp.Temp r) {
	  return n.temp;
    }

    temp.Temp munchExp(tree.BINOP n, temp.Temp r) {
	  temp.Temp reg = null;
	  reg = (r == null) ? new temp.Temp() : r;
	  String operation = "\t";

	  switch(n.binop) {
	    case tree.BINOP.PLUS: {
		  operation += "add\t";
		  break;
	    }
	    case tree.BINOP.MINUS: {
		  operation += "sub\t";
		  break;
	    }
	    case tree.BINOP.MUL: {
		  operation += "smul\t";
		  break;
	    }
	    case tree.BINOP.LSHIFT: {
		  operation += "sll\t";
		  break;
	    }
	    case tree.BINOP.AND: {
		  operation += "and\t";
		  break;
	    }
	    case tree.BINOP.XOR: {
		  operation += "xor\t";
		  break;
	    }
	  }

	  if(n.left instanceof tree.CONST) {
	    tree.CONST left = ((tree.CONST)n.left);
	    if(is13bitCONST(left))
		  eOPER(operation + "`s0, " + left.value + ", `d0\n", L(reg, null), L(munchExp(n.right, reg), null));
	    else {
		  eOPER("\tsethi\t%hi(" + left.value  +"), `d0\n", L(reg, null), null);
		  eOPER("\tor\t`s0, %lo(" + left.value + "), `d0\n", L(reg, null), L(reg, null));
		  eOPER(operation + "`s0, `s1, `d0\n", L(reg, null), L(reg, L(munchExp(n.right, transient1), null)));
	    }
	  }
	  else if(n.right instanceof tree.CONST) {
	    tree.CONST right = ((tree.CONST)n.right);
	    if(is13bitCONST(right))
		  eOPER(operation + "`s0, " + right.value + ", `d0\n", L(reg, null), L(munchExp(n.left, reg), null));
	    else {
		  eOPER("\tsethi\t%hi(" + right.value  +"), `d0\n", L(reg, null), null);
		  eOPER("\tor\t`s0, %lo(" + right.value + "), `d0\n", L(reg, null), L(transient1, null));
		  eOPER(operation + "`s1, `s0, `d0\n", L(reg, null), L(reg, L(munchExp(n.left, transient1), null)));
	    }
	  }
	  else
	    eOPER(operation + "`s0, `s1, `d0\n", L(reg, null), L(munchExp(n.left, transient1), L(munchExp(n.right, transient2),null)));

	  return reg;
    }

    temp.Temp munchExp(tree.MEM n, temp.Temp r) {
	  if(n.exp instanceof tree.TEMP) {
	    return ((tree.TEMP)n.exp).temp;
	  }

	  temp.Temp reg;
	  reg = (r == null) ? new temp.Temp() : r;
	  if(n.exp instanceof tree.BINOP) {
	    tree.BINOP bexp = (tree.BINOP)n.exp;
	    if(bexp.binop == tree.BINOP.PLUS) {
		  if(bexp.left instanceof tree.CONST) {
		    tree.CONST left = (tree.CONST)bexp.left;
		    eOPER("\tld\t[`s0 + " + left.value  + "], `d0\n", L(reg, null), L(munchExp(bexp.right, reg), null));
		  }
		  else if(bexp.right instanceof tree.CONST) {
		    tree.CONST right = (tree.CONST)bexp.right;
		    eOPER("\tld\t[`s0 + " + right.value  + "], `d0\n", L(reg, null), L(munchExp(bexp.left, reg), null));
		  }
		  else
		    eOPER("\tld\t[`s0 + `s1], `d0\n", L(reg, null), L(munchExp(bexp.left),
								      L(munchExp(bexp.right),
									null)));
	    }
	  }
	  else
	    throw new Error("Error");

	  return reg;
    }

    temp.Temp munchExp(tree.CALL n, temp.Temp r) {
	  temp.Temp reg;
	  reg = (r == null) ? frame.RVCaller() : r;

	  int argCount = 0;
	  int spCount = 92;
	  tree.ExpList args = n.args;
	  while(args != null) {
	    if( argCount <= 5) {
		  if(args.head instanceof tree.TEMP) {
		    tree.TEMP currArg = (tree.TEMP)args.head;
		    eOPER("\tmov\t`s0, `d0\n", L(frame.outgoingArgs[argCount++], null), L(currArg.temp, null));
		  }
		  else if(args.head instanceof tree.MEM) {
		    tree.MEM location = (tree.MEM)args.head;
		    if(location.exp instanceof tree.BINOP) {
			  tree.BINOP bexp = (tree.BINOP)location.exp;
			  if(bexp.binop == tree.BINOP.PLUS) {
			    if(bexp.left instanceof tree.CONST) {
				  tree.CONST left = (tree.CONST)bexp.left;
				  eOPER("\tld\t[`s0 + " + left.value  + "], `d0\n", L(frame.outgoingArgs[argCount++], null), L(munchExp(bexp.right, transient3), null));
			    }
			    else if(bexp.right instanceof tree.CONST) {
				  tree.CONST right = (tree.CONST)bexp.right;
				  eOPER("\tld\t[`s0 + " + right.value  + "], `d0\n", L(frame.outgoingArgs[argCount++], null), L(munchExp(bexp.left, transient3), null));
			    }
			    else
				  eOPER("\tld\t[`s0 + `s1], `d0\n", L(frame.outgoingArgs[argCount++], null), L(munchExp(bexp.left, transient3),
													     L(munchExp(bexp.right, transient2),
													       null)));
			  }
		    }
		    else
			  eOPER("\tld\t[`s0 + `s1], `d0\n", L(frame.outgoingArgs[argCount++], null),  L(munchExp(args.head, transient3), L(frame.g0, null)));
		  }
		  else
		    munchExp(args.head, frame.outgoingArgs[argCount++]);
	    }
	    args = args.tail;
	  }
	  tree.NAME nm = (tree.NAME)n.func;
	  assem.InstrList tracer = null;
	  assem.InstrList currInstr = ilist;
	  while(currInstr.tail != null) {
	    tracer = currInstr;
	    currInstr = currInstr.tail;
	  }
	  if(tracer != null)
	    tracer.tail = new assem.InstrList(new assem.OPER("\tcall `j0\n", null, null, new temp.LabelList(nm.label, null)), currInstr);
	  else {
	    eOPER("\tcall `j0\n", null, null, new temp.LabelList(nm.label, null));
	    eOPER("\tnop\n", null, null);
	  }

	  return reg;
    }

    void comment(String com) {
	  eOPER(com + "\n", null, null);
    }
}

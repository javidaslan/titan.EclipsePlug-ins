/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1;

import java.lang.ref.WeakReference;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenFactory;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.eclipse.core.resources.IFile;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.parsers.asn1parser.Asn1Lexer;
import org.eclipse.titan.designer.parsers.asn1parser.TokenWithIndexAndSubTokens;

/**
 * Represents a block of tokens.
 * <p>
 * In ASN.1 most of the tokens inside blocks can not be analyzed directly in
 * parse time. For this reason we are collecting them in such blocks, and when
 * the semantics are, these blocks are processed.
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class Block implements INamedNode, IVisitableNode, Token, TokenSource {
	/** the naming parent of the block. */
	//private WeakReference<INamedNode> nameParent;

	/**
	 * The location of the whole block. This location encloses the block
	 * fully, as it is used to report errors to.
	 **/
	private Location mLocation;

	/** the naming parent of the block. */
	private WeakReference<INamedNode> mNameParent;

	/**
	 * The list of the tokens contained inside the actual block. There might
	 * be sub-blocks in this list, but it does not contain its own '{' '}'
	 * enclosing tokens
	 * */
	private List<Token> tokenList;

	private int index = 0;

	/** How to create token objects */
	protected TokenFactory<?> _factory = CommonTokenFactory.DEFAULT;

	private Block( final Location aLocation ) {
		this.mLocation = aLocation;
	}

	public Block(final List<Token> tokenList, final Location location) {
		this( location );
		this.tokenList = tokenList;
	}

	public Block(final Token token) {
		if (token instanceof TokenWithIndexAndSubTokens) {
			tokenList = ((TokenWithIndexAndSubTokens) token).getSubTokens();
			final IFile sourceFile = ((TokenWithIndexAndSubTokens) token).getSourceFile();
			setLocation( new Location(sourceFile, token.getLine(), token.getStartIndex(), token.getStopIndex()) );
		} else {
			setLocation( NULL_Location.INSTANCE );
			tokenList = ((TokenWithIndexAndSubTokens) token).getSubTokens();
		}
	}

	/** @return the location of the block */
	public Location getLocation() {
		return mLocation;
	}

	public void setLocation( final Location aLocation ) {
		mLocation = aLocation;
	}

	@Override
	/** {@inheritDoc} */
	public String getFullName() {
		return getFullName(null).toString();
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		if (null != mNameParent) {
			final INamedNode tempParent = mNameParent.get();
			if (null != tempParent) {
				return tempParent.getFullName(this);
			}
		}

		return new StringBuilder();
	}

	@Override
	/** {@inheritDoc} */
	public void setFullNameParent(final INamedNode nameParent) {
		this.mNameParent = new WeakReference<INamedNode>(nameParent);
	}

	@Override
	/** {@inheritDoc} */
	public INamedNode getNameParent() {
		return mNameParent.get();
	}

	@Override
	/** {@inheritDoc} */
	public boolean accept(final ASTVisitor v) {
		switch (v.visit(this)) {
		case ASTVisitor.V_ABORT:
			return false;
		case ASTVisitor.V_SKIP:
			return true;
		}
		if (v.leave(this) == ASTVisitor.V_ABORT) {
			return false;
		}
		return true;
	}

	public int getTokenListSize() {
		return tokenList.size();
	}

	public List<Token> getTokenList() {
		return tokenList;
	}

	@Override
	/** {@inheritDoc} */
	public int getCharPositionInLine() {
		return tokenList.get(0).getCharPositionInLine();
	}

	@Override
	/** {@inheritDoc} */
	public CharStream getInputStream() {
		assert(false);
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public int getLine() {
		return tokenList.get(0).getLine();
	}

	@Override
	/** {@inheritDoc} */
	public int getChannel() {
		return Token.DEFAULT_CHANNEL;
	}

	@Override
	/** {@inheritDoc} */
	public int getStartIndex() {
		return tokenList.get(0).getStartIndex();
	}

	@Override
	/** {@inheritDoc} */
	public int getStopIndex() {
		return tokenList.get(tokenList.size() - 1).getStopIndex();
	}

	@Override
	/** {@inheritDoc} */
	public String getText() {
		final StringBuilder text = new StringBuilder();
		for (final Token t : tokenList) {
			text.append(t.getText());
		}
		return text.toString();
	}

	@Override
	/** {@inheritDoc} */
	public int getTokenIndex() {
		assert(false);
		return -1;
	}

	@Override
	/** {@inheritDoc} */
	public TokenSource getTokenSource() {
		assert(false);
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public int getType() {
		return Asn1Lexer.BLOCK;
	}

	@Override
	/** {@inheritDoc} */
	public String getSourceName() {
		return IntStream.UNKNOWN_SOURCE_NAME;
	}

	@Override
	/** {@inheritDoc} */
	public TokenFactory<?> getTokenFactory() {
		return _factory;
	}

	@Override
	/** {@inheritDoc} */
	public Token nextToken() {
		return tokenList.get(index++);
	}

	@Override
	/** {@inheritDoc} */
	public void setTokenFactory(final TokenFactory<?> factory) {
		this._factory = factory;
	}
}

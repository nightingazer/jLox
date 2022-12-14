package com.makinginterpreters.jlox;

import java.util.ArrayList;
import java.util.List;

import static com.makinginterpreters.jlox.TokenType.*;

public class Parser {

	private static class ParseError extends RuntimeException {
	}

	static ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}

	private final List<Token> tokens;
	private int current = 0;

	public Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	private void synchronize() {
		advance();

		while (!isAtEnd()) {
			if (previous().type == SEMICOLON) return;

			switch (peek().type) {
				case CLASS:
				case FUN:
				case VAR:
				case FOR:
				case IF:
				case WHILE:
				case PRINT:
				case RETURN:
					return;
			}

			advance();
		}
	}

	public List<Stmt> parse() {
		try {
			List<Stmt> statements = new ArrayList<>();
			while (!isAtEnd()) {
				statements.add(declaration());
			}
			return statements;
		} catch (ParseError error) {
			return null;
		}
	}

	private Stmt declaration() {
		try{
			if (match(VAR)) return varDeclaration();
			return statement();
		} catch (ParseError e) {
			synchronize();
			return null;
		}
	}

	private Stmt varDeclaration() {
		Token name = consume(IDENTIFIER, "Expect a variable name.");
		Expr initializer = null;
		if(match(EQUAL))  {
			initializer = expression();
		}

		consume(SEMICOLON, "Expeceted \";\" at the end of expression.");
		return new Stmt.Var(name, initializer);
	}

	private Stmt statement() {
		if (match(PRINT)) return printStatement();
		return expressionStatement();
	}

	private Stmt expressionStatement() {
		Expr value = expression();
		consume(SEMICOLON, "Expect \";\" after expression.");
		return new Stmt.Expression(value);
	}

	private Stmt printStatement() {
		Expr value = expression();
		consume(SEMICOLON, "Expect \";\" after expression.");
		return new Stmt.Print(value);
	}


	private Expr expression() {
		return assignment();
	}

	private Expr assignment() {
		Expr expr = equality();

		if(match(EQUAL)) {
			Token equals = previous();
			Expr value = assignment();

			if(expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				return new Expr.Assign(name, value);
			}

			throw error(equals, "Invalid assignment target");
		}

		return expr;
	}

	private Expr equality() {
		Expr expr = comparison();

		while (match(BANG_EQUAL, EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr comparison() {
		Expr expr = term();

		while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr term() {
		Expr expr = factor();

		while (match(MINUS, PLUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr factor() {
		Expr expr = unary();

		while (match(SLASH, STAR)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr unary() {
		if (match(BANG, MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}
		return primary();
	}

	private Expr primary() {
		if (match(FALSE)) return new Expr.Literal(false);
		if (match(TRUE)) return new Expr.Literal(true);
		if (match(NIL)) return new Expr.Literal(null);

		if (match(STRING, NUMBER)) return new Expr.Literal(previous().literal);

		if(match(IDENTIFIER)) return new Expr.Variable(previous());

		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect \")\" after expression.");
			return new Expr.Grouping(expr);
		}
		throw error(peek(), "Expected expression.");
	}

	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}
		return false;
	}

	private boolean check(TokenType type) {
		if (isAtEnd()) return false;
		return peek().type == type;
	}

	private Token advance() {
		if (!isAtEnd()) current++;
		return previous();
	}

	private boolean isAtEnd() {
		return peek().type == EOF;
	}

	private Token peek() {
		return tokens.get(current);
	}

	private Token previous() {
		return tokens.get(current - 1);
	}

	private Token consume(TokenType type, String message) {
		if (check(type)) return advance();
		throw error(peek(), message);
	}
}
